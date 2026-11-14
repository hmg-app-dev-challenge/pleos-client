"""Run: python3 -m unittest discover -s server -v (isolated temporary database)."""
import importlib.util
import json
import os
import tempfile
import threading
import unittest
import urllib.request
import urllib.error
from http.server import ThreadingHTTPServer
from pathlib import Path

class ApiTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        os.environ['CHAPARK_DATA_DIR'] = cls.temp.name
        spec = importlib.util.spec_from_file_location('isolated_api', Path(__file__).with_name('app.py'))
        cls.api = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(cls.api)
        del os.environ['CHAPARK_DATA_DIR']
        cls.server = ThreadingHTTPServer(('127.0.0.1', 0), cls.api.Handler)
        cls.worker = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.worker.start()
    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown(); cls.server.server_close(); cls.worker.join()
        cls.api.DB.close(); cls.temp.cleanup()
    def request(self, method, path, body=None, token=True):
        req = urllib.request.Request('http://127.0.0.1:%s%s' % (self.server.server_port, path),
            data=json.dumps(body).encode() if body is not None else None, method=method,
            headers={'Authorization': 'Bearer ' + self.api.TOKEN} if token else {})
        try: response = urllib.request.urlopen(req, timeout=3)
        except urllib.error.HTTPError as error: response = error
        with response: return response.status, json.load(response)
    def test_authentication_required_for_read_and_write(self):
        for method, path in [('GET','/health'),('GET','/v1/records'),('PUT','/v1/records/a'),('DELETE','/v1/records/a')]:
            self.assertEqual(401, self.request(method,path,token=False)[0])
    def test_retry_conflict_and_delete_tombstone(self):
        path='/v1/records/retry-test'
        record={'placeId':'camp-nanji','records':[{'kind':'ARRIVAL','origin':'USER'}]}
        first=self.request('PUT',path,record)
        self.assertEqual(200,first[0])
        self.assertEqual(first,self.request('PUT',path,record))
        self.assertEqual(409,self.request('PUT',path,dict(record,people=3))[0])
        rows=self.request('GET','/v1/records')[1]['records']
        self.assertEqual(record,next(r['data'] for r in rows if r['id']=='retry-test'))
        self.assertEqual(200,self.request('DELETE',path)[0])
        self.assertEqual(200,self.request('DELETE',path)[0])
        self.assertEqual(410,self.request('PUT',path,record)[0])
        self.assertFalse(any(r['id']=='retry-test' for r in self.request('GET','/v1/records')[1]['records']))
    def test_invalid_record_and_identifier(self):
        self.assertEqual(400,self.request('PUT','/v1/records/invalid',{'records':[]})[0])
        for method in ['PUT','DELETE']:
            self.assertEqual(400,self.request(method,'/v1/records/a/b',{})[0])

    def test_plan_revision_retry_conflict_and_deletion(self):
        path='/v1/plans/plan-test'
        first={'baseRevision':0,'data':{'schemaVersion':1,'placeId':'camp-nanji','people':3}}
        self.assertEqual(401,self.request('PUT',path,first,token=False)[0])
        self.assertEqual(1,self.request('PUT',path,first)[1]['revision'])
        self.assertEqual(1,self.request('PUT',path,first)[1]['revision'])
        second={'baseRevision':0,'data':dict(first['data'],people=4)}
        self.assertEqual(409,self.request('PUT',path,second)[0])
        second['baseRevision']=1
        self.assertEqual(2,self.request('PUT',path,second)[1]['revision'])
        self.assertEqual(4,self.request('GET',path)[1]['data']['people'])
        self.assertTrue(any(p['id']=='plan-test' for p in self.request('GET','/v1/plans')[1]['plans']))
        self.assertEqual(401,self.request('GET','/v1/plans',token=False)[0])
        self.assertEqual(200,self.request('DELETE',path)[0])
        self.assertFalse(any(p['id']=='plan-test' for p in self.request('GET','/v1/plans')[1]['plans']))
        self.assertEqual(410,self.request('GET',path)[0])
        self.assertEqual(410,self.request('PUT',path,second)[0])

    def test_plan_v2_keeps_three_cooking_modes_and_rejects_unknown(self):
        for mode in ('NONE','SIMPLE','COOKING'):
            body={'baseRevision':0,'data':{'schemaVersion':2,'placeId':'camp-nanji','cookingPlan':mode}}
            path='/v1/plans/v2-'+mode
            self.assertEqual(200,self.request('PUT',path,body)[0])
            self.assertEqual(mode,self.request('GET',path)[1]['data']['cookingPlan'])
        invalid={'baseRevision':0,'data':{'schemaVersion':2,'placeId':'camp-nanji','cookingPlan':'BOGUS'}}
        self.assertEqual(400,self.request('PUT','/v1/plans/v2-invalid',invalid)[0])

    def test_catalog_and_rule_contracts_are_versioned_and_authenticated(self):
        self.assertEqual(401,self.request('GET','/v1/catalog',token=False)[0])
        catalog=self.request('GET','/v1/catalog')[1]
        rules=self.request('GET','/v1/rules')[1]
        self.assertEqual(1,catalog['schemaVersion'])
        self.assertEqual(rules['version'],catalog['ruleVersion'])
        self.assertEqual(4,len(catalog['places']))
        self.assertEqual(4,len({p['id'] for p in catalog['places']}))

    def test_memory_metadata_round_trips_without_rewriting_completed_snapshot(self):
        path='/v1/records/memory-v02'
        record={'placeId':'camp-nanji','placeName':'난지캠핑장',
                'memory':{'version':1,'sleepChecked':6,'sleepRequired':6,'exitChecked':5,'exitRequired':5,
                          'chargerSelected':False,'completed':True},
                'records':[{'kind':'PLACE_CONTEXT','origin':'REFERENCE','placeMemory':{
                    'sceneryTags':['강'],'informationDates':[],'hasUndatedInfo':True}},
                    {'kind':'TRIP_COMPLETED','origin':'USER','readingStatus':'DISCONNECTED','source':'SDK'}]}
        self.assertEqual(200,self.request('PUT',path,record)[0])
        rows=self.request('GET','/v1/records')[1]['records']
        self.assertEqual(record,next(r['data'] for r in rows if r['id']=='memory-v02'))
        changed=dict(record,memory=dict(record['memory'],sleepChecked=5))
        self.assertEqual(409,self.request('PUT',path,changed)[0])

if __name__=='__main__': unittest.main()
