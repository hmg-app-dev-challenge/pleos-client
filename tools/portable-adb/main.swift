// Local-only ADB transport for the standalone launcher. No global adb server/key changes.
import Foundation
import Security
import Darwin

struct Failure: Error, CustomStringConvertible { let description: String; init(_ s:String){description=s} }
func word(_ value:UInt32)->Data { var v=value.littleEndian;return withUnsafeBytes(of:&v){Data($0)} }
func number(_ data:Data,_ offset:Int)->UInt32 {
    let b=Array(data[offset..<offset+4]);return UInt32(b[0]) | UInt32(b[1])<<8 | UInt32(b[2])<<16 | UInt32(b[3])<<24
}
func command(_ s:String)->UInt32 { number(Data(s.utf8),0) }
struct Message { let code:UInt32;let a:UInt32;let b:UInt32;let data:Data }
func element(_ bytes:[UInt8],_ cursor:inout Int)throws->(UInt8,[UInt8]) {
    guard cursor+2<=bytes.count else {throw Failure("Invalid key DER")}
    let tag=bytes[cursor];cursor+=1
    var length=Int(bytes[cursor]);cursor+=1
    if length&128 != 0 {
        let count=length&127;length=0
        guard count>0 && count<=4 && cursor+count<=bytes.count else{throw Failure("Invalid key length")}
        for _ in 0..<count {length=length*256+Int(bytes[cursor]);cursor+=1}
    }
    guard length<=bytes.count-cursor else{throw Failure("Truncated key")}
    let value=Array(bytes[cursor..<cursor+length]);cursor+=length;return(tag,value)
}
func readKey(_ path:String)throws->SecKey {
    let pem=try String(contentsOfFile:path,encoding:.utf8)
    let base64=pem.split(separator:"\n").filter{!$0.hasPrefix("-----")}.joined()
    guard var der=Data(base64Encoded:base64,options:.ignoreUnknownCharacters) else{throw Failure("Invalid ADB key format")}
    if !pem.contains("BEGIN RSA PRIVATE KEY") {
        var cursor=0
        let root=try element(Array(der),&cursor)
        cursor=0;_ = try element(root.1,&cursor);_ = try element(root.1,&cursor)
        let privateKey=try element(root.1,&cursor)
        guard privateKey.0==4 else{throw Failure("Unsupported ADB key format")}
        der=Data(privateKey.1)
    }
    var error:Unmanaged<CFError>?
    let attributes:[CFString:Any]=[kSecAttrKeyType:kSecAttrKeyTypeRSA,kSecAttrKeyClass:kSecAttrKeyClassPrivate,kSecAttrKeySizeInBits:2048]
    guard let key=SecKeyCreateWithData(der as CFData,attributes as CFDictionary,&error) else{throw Failure("Cannot import ADB key")}
    return key
}
final class Connection {
    let fd:Int32
    var remote:UInt32=0
    var pending=Data()
    var maximum=65536
    let local:UInt32=1
    init(port:UInt16,keys:[String])throws {
        fd=socket(AF_INET,SOCK_STREAM,0)
        guard fd>=0 else{throw Failure("Cannot create local socket")}
        var time=timeval(tv_sec:10,tv_usec:0)
        setsockopt(fd,SOL_SOCKET,SO_RCVTIMEO,&time,socklen_t(MemoryLayout.size(ofValue:time)))
        setsockopt(fd,SOL_SOCKET,SO_SNDTIMEO,&time,socklen_t(MemoryLayout.size(ofValue:time)))
        var noSignal:Int32=1
        setsockopt(fd,SOL_SOCKET,SO_NOSIGPIPE,&noSignal,socklen_t(MemoryLayout.size(ofValue:noSignal)))
        var address=sockaddr_in();address.sin_len=UInt8(MemoryLayout<sockaddr_in>.size)
        address.sin_family=sa_family_t(AF_INET);address.sin_port=port.bigEndian
        address.sin_addr.s_addr=inet_addr("127.0.0.1")
        let connected=withUnsafePointer(to:&address){p in p.withMemoryRebound(to:sockaddr.self,capacity:1){Darwin.connect(fd,$0,socklen_t(MemoryLayout<sockaddr_in>.size))}}
        guard connected==0 else{throw Failure("Cannot connect to local emulator")}
        let signingKeys=try keys.map{try readKey($0)}
        try send("CNXN",0x01000000,65536,Data("host::\0".utf8))
        var index=0;var sentPublic=false
        while true {
            let message=try receive()
            if message.code==command("CNXN") {guard message.b>0 else{throw Failure("Invalid ADB payload limit")};maximum=min(Int(message.b),65536);return}
            guard message.code==command("AUTH") && message.a==1 else{throw Failure("Unexpected ADB handshake")}
            if index<signingKeys.count {
                var error:Unmanaged<CFError>?
                guard let signature=SecKeyCreateSignature(signingKeys[index],.rsaSignatureDigestPKCS1v15SHA1,message.data as CFData,&error) else{throw Failure("ADB signing failed")}
                index+=1;try send("AUTH",2,0,signature as Data)
            } else {
                guard !sentPublic else{throw Failure("Approve USB debugging in the emulator")}
                let publicKey=try String(contentsOfFile:keys[0]+".pub",encoding:.utf8).trimmingCharacters(in:.whitespacesAndNewlines)
                try send("AUTH",3,0,Data((publicKey+"\0").utf8));sentPublic=true
            }
        }
    }
    deinit {Darwin.close(fd)}
    func writeAll(_ data:Data)throws {
        try data.withUnsafeBytes { raw in
            var offset=0
            while offset<raw.count {
                let sent=Darwin.write(fd,raw.baseAddress!.advanced(by:offset),raw.count-offset)
                if sent<0 && errno==EINTR {continue}
                guard sent>0 else{throw Failure("ADB write timed out or disconnected")};offset+=sent
            }
        }
    }
    func readAll(_ count:Int)throws->Data {
        if count==0{return Data()}
        var bytes=[UInt8](repeating:0,count:count);var offset=0
        try bytes.withUnsafeMutableBytes { raw in
            while offset<count {
                let got=Darwin.read(fd,raw.baseAddress!.advanced(by:offset),count-offset)
                if got<0 && errno==EINTR {continue}
                guard got>0 else{throw Failure("ADB read timed out; check emulator/debugging approval")};offset+=got
            }
        }
        return Data(bytes)
    }
    func send(_ name:String,_ a:UInt32,_ b:UInt32,_ data:Data=Data())throws {
        let code=command(name);let sum=data.reduce(UInt32(0)){$0 &+ UInt32($1)}
        try writeAll(word(code)+word(a)+word(b)+word(UInt32(data.count))+word(sum)+word(code ^ UInt32.max)+data)
    }
    func receive()throws->Message {
        let header=try readAll(24);let code=number(header,0);let length=number(header,12)
        guard code ^ number(header,20)==UInt32.max && length<=16*1024*1024 else{throw Failure("Invalid ADB frame")}
        let body=try readAll(Int(length))
        guard body.reduce(UInt32(0),{$0 &+ UInt32($1)})==number(header,16) else{throw Failure("Invalid ADB checksum")}
        return Message(code:code,a:number(header,4),b:number(header,8),data:body)
    }
    func open(_ service:String)throws {
        try send("OPEN",local,0,Data((service+"\0").utf8))
        let reply=try receive()
        guard reply.code==command("OKAY") && reply.b==local else{throw Failure("ADB service rejected")}
        remote=reply.a
    }
    func receiveData()throws->Bool {
        let m=try receive()
        guard m.b==local && m.a==remote else{throw Failure("Unexpected ADB stream")}
        if m.code==command("CLSE") {try send("CLSE",local,remote);return false}
        guard m.code==command("WRTE") else{throw Failure("Unexpected ADB response")}
        pending.append(m.data);try send("OKAY",local,remote);return true
    }
    func writeStream(_ data:Data)throws {
        var offset=0
        while offset<data.count {
            let end=min(data.count,offset+maximum)
            try send("WRTE",local,remote,data.subdata(in:offset..<end));offset=end
            while true {
                let m=try receive()
                guard m.b==local && m.a==remote else{throw Failure("Unexpected ADB stream acknowledgement")}
                if m.code==command("OKAY"){break}
                guard m.code==command("WRTE") else{throw Failure("ADB stream closed while writing")}
                pending.append(m.data);try send("OKAY",local,remote)
            }
        }
    }
    func shell(_ value:String)throws->Data {try open("shell:"+value);while try receiveData(){};return pending}
    func push(_ file:String,_ target:String)throws {
        try open("sync:")
        let name=Data((target+",33206").utf8)
        try writeStream(Data("SEND".utf8)+word(UInt32(name.count))+name)
        let handle=try FileHandle(forReadingFrom:URL(fileURLWithPath:file));defer{try? handle.close()}
        while let chunk=try handle.read(upToCount:65536), !chunk.isEmpty {
            try writeStream(Data("DATA".utf8)+word(UInt32(chunk.count))+chunk)
        }
        try writeStream(Data("DONE".utf8)+word(UInt32(Date().timeIntervalSince1970)))
        while pending.count<8 {guard try receiveData() else{throw Failure("ADB sync closed early")}}
        guard String(data:pending.prefix(4),encoding:.utf8)=="OKAY" else{throw Failure("ADB file upload failed")}
        try send("CLSE",local,remote)
    }
}
func run()throws {
    let args=Array(CommandLine.arguments.dropFirst())
    guard args.count>=5,let port=UInt16(args[0]),let separator=args.firstIndex(of:"--"),separator>=2 else{throw Failure("Usage: portable-adb PORT KEY [KEY...] -- shell COMMAND | install USER APK")}
    let keys=Array(args[1..<separator]);let action=Array(args[(separator+1)...])
    guard !action.isEmpty else{throw Failure("Missing command")}
    if action[0]=="shell",action.count==2 {
        let data=try Connection(port:port,keys:keys).shell(action[1]);FileHandle.standardOutput.write(data)
    } else if action[0]=="install",action.count==3,UInt32(action[1]) != nil {
        let remote="/data/local/tmp/chapark-"+UUID().uuidString+".apk"
        try Connection(port:port,keys:keys).push(action[2],remote)
        let result=try Connection(port:port,keys:keys).shell("pm install -r --user \(action[1]) \(remote); rm -f \(remote)")
        FileHandle.standardOutput.write(result)
        guard String(data:result,encoding:.utf8)?.contains("Success")==true else{throw Failure("APK installation failed")}
    } else{throw Failure("Invalid command")}
}
do {try run()} catch {FileHandle.standardError.write(Data("\(error)\n".utf8));exit(1)}
