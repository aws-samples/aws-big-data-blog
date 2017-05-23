#!/usr/bin/python
import MySQLdb
import random
import datetime

db = MySQLdb.connect(host="AURORA_CNAME",
                     user="DBUSER",
                     passwd="DBPASSWORD",
                     db="DB")

states = ("AL","AK","AZ","AR","CA","CO","CT","DE","FL","GA","HI","ID","IL","IN",
"IA","KS","KY","LA","ME","MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ",
"NM","NY","NC","ND","OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT","VA",
"WA","WV","WI","WY")

shipping_types = ("Free", "3-Day", "2-Day")

product_categories = ("Garden", "Kitchen", "Office", "Household")
referrals = ("Other", "Friend/Colleague", "Repeat Customer", "Online Ad")

for i in range(0,10):
    item_id = random.randint(1,100)
    state = states[random.randint(0,len(states)-1)]
    shipping_type = shipping_types[random.randint(0,len(shipping_types)-1)]
    product_category = product_categories[random.randint(0,len(product_categories)-1)]
    quantity = random.randint(1,4)
    referral = referrals[random.randint(0,len(referrals)-1)]
    price = random.randint(1,100)
    order_date = datetime.date(2016,random.randint(1,12),random.randint(1,30)).isoformat()

    data_order = (item_id, product_category, price, quantity, order_date, state,
    shipping_type, referral)

    add_order = ("INSERT INTO Sales "
                   "(ItemID, Category, Price, Quantity, OrderDate, DestinationState, \
                   ShippingType, Referral) "
                   "VALUES (%s, %s, %s, %s, %s, %s, %s, %s)")

    cursor = db.cursor()
    cursor.execute(add_order, data_order)

    db.commit()

cursor.close()
db.close()
