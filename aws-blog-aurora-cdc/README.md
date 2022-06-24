# Capturing data change in aurora and visualizing data in QuickSight
This code generates sample data in a MySQL database table.

## Prerequisites
  - Amazon Web Services account
  - [AWS Command Line Interface (CLI)]
  - Python 3.5+
  - Python MySQLdb


### Running example
You can run the example my replacing the variable for Database connection in the python script with your database details. You will need
  - Database CName
  - Database User
  - Database Password
  - Database Name
You can also change the number of records the script generates.

Running the script:
```
  python RDS_Data_Generator.py
```
[AWS Command Line Interface (CLI)]:http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-welcome.html
