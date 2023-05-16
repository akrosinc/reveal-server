update campaign_drug
set drugs = '[
  {
    "name": "Vaccine",
    "min": 0.1,
    "max": 2.0,
    "half": false,
    "full": false,
    "millis": true,
    "key": "vc"
  }
]'
WHERE name = 'Vaccination';