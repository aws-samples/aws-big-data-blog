#!/usr/bin/env python

import uuid
from time import strftime
import random
from faker import Faker
import boto.dynamodb

conn = boto.dynamodb.connect_to_region('<Region_Name>')
table = conn.get_table('<DynamoDB_Table_Name>')

fake = Faker()
shippingmethodarray = ['1-hour','1-day','2-days','1-hour','1-day','1-hour','1-day','1-hour','1-day','1-day','1-day','2-day','1-week']
orderqtyarray = [1,2,2,3,1,1,2,2,2,3,4,5,6,7,1,1,1,1,1,1,1,1,2]
priceRange = ['1,10','1,10','1,10','1,10','11,20','11,20','21,30','31,40','11,20','11,20','11,20','21,30','11,20','21,30','41,50','51,60','61,70','71,80','81,90','11,20','1,10']
productCategoryarray = ['Toys','Books','Movies','Electronics','Clothing','Sports','Healthcare','Toys','Electronics','Electronics','Electronics','Toys','Healthcare']

while 1:
	orderId = str(uuid.uuid4())
	orderDate = int(strftime("%Y%m%d%H%M%S"))
	shipMethod = str(random.choice(shippingmethodarray))
	fullAddress = str(fake.address())
	billAddress = str(fullAddress).replace('\n',' ').replace(',',' ')
	billCity = str(fullAddress.rsplit(' ', 1)[-2].split(' ')[-1])
	billPostalCode = int(fullAddress.rsplit(' ', 1)[-1].split('-')[0])
	orderQty = int(random.choice(orderqtyarray))
	unitPriceRange = random.choice(priceRange)
	unitPrice = random.randint(int(unitPriceRange.split(',')[0]),int(unitPriceRange.split(',')[1]))
	productCategory = str(random.choice(productCategoryarray))
	item_data = {
		'OrderDate' : orderDate,
		'ShipMethod' : shipMethod,
		'BillAddress' : billAddress,
		'BillCity' : billCity,
		'BillPostalCode' : billPostalCode,
		'OrderQty' : orderQty,
		'UnitPrice' : unitPrice,
		'ProductCategory' : productCategory
	}
	item = table.new_item(
		hash_key = orderId,
		attrs=item_data
	)

	item.put()