/*
Question 1: Correct
PreRequiste:[Prerequisite section will not be there in actual exam. Your exam environment will already be setup with required data]
Run below sqoop command

sqoop import \
--connect "jdbc:mysql://localhost/retail_db" \
--password cloudera \
--username root \
--table customers \
--fields-terminated-by '\t' \
--columns "customer_id,customer_fname,customer_city" \
--target-dir /user/cloudera/problem9/customer_text

Instructions:
Create a metastore table named customer_parquet_compressed from tab delimited files provided at below location.
Input folder is /user/cloudera/problem9/customer-text
Schema for input file
customer_id customer_fname customer_city

Output Requirement:
Use this location to store data for hive table: /user/cloudera/problem9/customer-hive
Output file should be saved in parquet format using Snappy compression.

Important Information
I have provided the solution using Hive. You can also solve it using Spark+Hive.
For compression, below Hive property should be set to true
SET hive.exec.compress.output=true;
*/
sqoop import \
--connect "jdbc:mysql://localhost/retail_db" \
  --password cloudera \
  --username root \
  --table customers \
  --fields-terminated-by '\t' \
  --columns "customer_id,customer_fname,customer_city" \
  --delete-target-dir \
  --target-dir /user/cloudera/problem9/customer_text \
  --outdir /home/cloudera/outdir \
--bindir /home/cloudera/bindir \
--num-mappers 8

//USING SPARK + HIVE
val customers = sc.textFile("/user/cloudera/problem9/customer_text").map(line => line.split('\t')).map(r => (r(0).toInt,r(1),r(2))).toDF("customer_id","customer_fname","customer_city")
customers.show()
sqlContext.setConf("spark.sql.parquet.compression.codec","snappy")
customers.write.parquet("/user/cloudera/problem9/customer-hive")

sqlContext.sql("""create table customer_parquet_compressed(customer_id int,customer_fname string,customer_city string) stored as parquet location '/user/cloudera/problem9/customer-hive' tblproperties("parquet.compression"="snappy")""")

//USING HIVE
CREATE TABLE customer_temp(id int,name string,city string) ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t' LOCATION '/user/cloudera/problem9/customer_text';
CREATE TABLE customer_parquet_compressed STORED AS PARQUET LOCATION '/user/cloudera/problem9/customer-hive' TBLPROPERTIES("parquet.compression"="SNAPPY") AS SELECT * FROM customer_temp;

//CHECKING THE RESULTS
$ hdfs dfs -ls /user/cloudera/problem9/customer-hive
$ parquet-tools meta hdfs://quickstart.cloudera/user/cloudera/problem9/customer-hive/part-r-00000-4b550e0c-6924-4d70-8ee2-a2bbfaf5ee92.snappy.parquet

hive> use default;
hive> show tables;
hive> describe formatted customer_parquet_compressed;
hive> select * from customer_parquet_compressed limit 10;