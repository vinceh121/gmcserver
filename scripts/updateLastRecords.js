db.devices.aggregate([
{
    '$lookup': {
      'from': 'records', 
      'as': 'lastRecordId', 
      'let': {
        'devId': '$_id'
      }, 
      'pipeline': [
        {
          '$match': {
            '$expr': {
              '$eq': [
                '$deviceId', '$$devId'
              ]
            }
          }
        }, {
          '$sort': {
            'date': -1
          }
        }, {
          '$limit': 1
        }
      ]
    }
  }, {
    '$unwind': {
      'path': '$lastRecordId'
    }
  }
]).forEach(r => db.devices.updateOne({_id: r._id}, {$set:{lastRecordId: r.lastRecordId._id}}));

