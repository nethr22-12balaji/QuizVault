import requests
import time
import os

BASE = "http://localhost:8080/api"

def api(ep, data=None, token=None):
    headers = {}
    if token: headers['X-Token'] = token
    if data:
        r = requests.post(BASE + ep, data=data, headers=headers)
    else:
        r = requests.get(BASE + ep, headers=headers)
    if r.status_code == 200:
        return r.json()
    return {'status': r.status_code}

print("Running tests...")

# 1. Register QM and 3 Participants
res = api('/register', {'username':'qm', 'password':'qm', 'role':'Quizmaster'})
assert res.get('success'), "QM register failed"
tokens = {}
for u in ['p1', 'p2', 'p3']:
    res = api('/register', {'username':u, 'password':'p', 'role':'Participant'})
    assert res.get('success'), f"Participant {u} register failed"

# Login
t_qm = api('/login', {'username':'qm', 'password':'qm'})['token']
t_p1 = api('/login', {'username':'p1', 'password':'p'})['token']
t_p2 = api('/login', {'username':'p2', 'password':'p'})['token']
t_p3 = api('/login', {'username':'p3', 'password':'p'})['token']
tokens = {'qm':t_qm, 'p1':t_p1, 'p2':t_p2, 'p3':t_p3}

# 2. Create teams
api('/teams/create', {'name':'TeamA'}, token=t_p1)
api('/teams/create', {'name':'TeamB'}, token=t_p2)
api('/teams/create', {'name':'TeamC'}, token=t_p3)

# 3. Add questions (12 Easy, 6 Med, 3 Hard)
for i in range(12): api('/questions/add', {'text':f'E{i}', 'answer':f'AnsE{i}', 'diff':'Easy'}, token=t_qm)
for i in range(6): api('/questions/add', {'text':f'M{i}', 'answer':f'AnsM{i}', 'diff':'Medium'}, token=t_qm)
for i in range(3): api('/questions/add', {'text':f'H{i}', 'answer':f'AnsH{i}', 'diff':'Hard'}, token=t_qm)

print("Setup complete")
