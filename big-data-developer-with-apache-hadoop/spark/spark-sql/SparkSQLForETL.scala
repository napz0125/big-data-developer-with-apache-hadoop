/**
 * Use Apache Spark SQL for ETL
 */

import org.apache.spark.sql._

/*
1. In a terminal window, use Sqoop to import the webpage table from MySQL.
   Use Parquet file format

$ sqoop import \
--connect jdbc:mysql://quickstart.cloudera/loudacre \
--username root \
--password cloudera \
--table webpage \
--delete-target-dir \
--target-dir /user/cloudera/loudacre/data/webpage \
--as-parquetfile \
--outdir /home/cloudera/outdir \
--bindir /home/cloudera/bindir \
--num-mappers 8

$ parquet-tools cat hdfs://quickstart.cloudera/user/cloudera/loudacre/data/webpage/6aef3171-24b3-4d60-9e21-f93051bcd0bc.parquet
*/

object SparkSQLForETL {

  def main(args: Array[String]): Unit = {

    val warehouseLocation = "hdfs://quickstart.cloudera/user/hive/warehouse"
    val spark = SparkSession.builder()
      .appName("Use Apache Spark SQL for ETL")
      .master("local[*]")
      .enableHiveSupport()
      .config("spark.sql.warehouse.dir",warehouseLocation)
      .getOrCreate()
    val sc = spark.sparkContext
    sc.setLogLevel("ERROR")

    import spark.implicits._

    // Create a DataFrame from the webpage table
    val webpageDF = spark
      .read
      .parquet("hdfs://quickstart.cloudera/user/cloudera/loudacre/data/webpage/")
      .cache

    // Show the schema for the webpage table dataframe
    webpageDF.printSchema()

    // View the first few records
    webpageDF.show(5)

    // Create a new DF by selecting thow columns of the first DF
    val assocFilesDF = webpageDF
      .select($"web_page_num",$"associated_files")
      .cache

    // Print the schema of the new DF
    assocFilesDF.printSchema

    // Show the first few rows of the DF
    assocFilesDF.show(5)

    // Create an RDD from the DF, and extract the column values from the Row items into a pair
    val aFilesRDD = assocFilesDF.map(row => (row.getAs[Int]("web_page_num"), row.getAs[String]("associated_files")))
    aFilesRDD.collect.foreach(println)

    // Split the list of files names in the second column
    val aFilesRDD2 = aFilesRDD.rdd.flatMapValues(filestring => filestring.split(","))

    // Convert the RDD to a RowRDD
    import org.apache.spark.sql.Row
    val aFilesRowRDD = aFilesRDD2.map(pair => Row(pair._1, pair._2))

    // Convert back to a DataFrame
    val aFileDF = spark.createDataFrame(aFilesRowRDD, assocFilesDF.schema).cache
    aFileDF.printSchema
    aFileDF.show(10)

    // Give the new DataFrame a more accurate column name
    val finalDF = aFileDF.withColumnRenamed("associated_files","associated_file")
    finalDF.printSchema
    finalDF.show(10)

    // Save as files, overwriting any prior data in the directory
    finalDF
      .write
      .mode("overwrite")
      .save("hdfs://quickstart.cloudera/user/cloudera/loudacre/results/webpage_files")

    // After saving, download and review one of the saved files
    // hdfs dfs -ls /user/cloudera/loudacre/results/webpage_files
    // $ parquet-tools schema hdfs://quickstart.cloudera/user/cloudera/loudacre/results/webpage_files/part-00000-d2ef46a7-34db-48a2-9f26-9970361be913-c000.snappy.parquet
    // parquet-tools head hdfs://quickstart.cloudera/user/cloudera/loudacre/results/webpage_files/part-00000-d2ef46a7-34db-48a2-9f26-9970361be913-c000.snappy.parquet

    sc.stop()
    spark.stop()
  }


/*SOLUTION IN THE SPARK REPL
//Importing Data from MySQL using Sqoop
sqoop import \
--connect jdbc:mysql://localhost/loudacre \
--username training \
--password training \
--table webpage \
--as-parquetfile \
--delete-target-dir \
--target-dir /loudacre/webpage \
--outdir /home/training/Desktop/outdir/ \
--bindir /home/training/Desktop/bindir/ \
--num-mappers 8

//Review the import
hdfs dfs -get /loudacre/webpage/1006a7c4-8c4c-4f07-868c-e1911d6a94c3.parquet /home/training/Desktop/files/webpage.parquet
parquet-tools schema /home/training/Desktop/files/webpage.parquet 
parquet-tools head /home/training/Desktop/files/webpage.parquet


//Creating a Dataframe from a table
val webpageDF = sqlContext.read.load("/loudacre/webpage")
//Examine the schema of the new DataFrame
webpageDF.printSchema()
//View the first few records in the table 
webpageDF.show(5)
//Querying a DataFrame
//Create a new DataFrame by selecting the web_page_num and associated_files columns from the existing DataFrame
val assocFilesDF = webpageDF.select($"web_page_num", $"associated_files")
//Wiew the schema and first few rows of the returned DataFrame to confirm that it was created correctly.
assocFilesDF.printSchema()
assocFilesDF.show(10)
//In order to manipulate the data using core Spark, convert the DataFrame into  a pair RDD using the map method.
//The input into the map method is a Row object.
//The key is the web_page_num value, and the value is the associated_files
val aFilesRDD = assocFilesDF.map(row => (row.getAs[Short]("web_page_num"), row.getAs[String]("associated_files")))
//Use flatMapValues to split and subtract the filenames in the associated_files column
val aFilesRDD2 = aFilesRDD.flatMapValues(filestring => filestring.split(","))
//Import the Row class and convert the pair RDD to a Row RDD.
import org.apache.spark.sql.Row
val aFilesRowRDD = aFilesRDD2.map(pair => Row(pair._1, pair._2))
//Convert the RDD back to a DataFrame, using the original DataFrame's schema
val aFileDF = sqlContext.createDataFrame(aFilesRowRDD, assocFilesDF.schema)
aFileDF.printSchema()
aFileDF.show(5)
//Create a new DataFrame with the associated_files column renamed to associated_file
val finalDF = aFileDF.withColumnRenamed("associated_files", "associated_file")
finalDF.printSchema()
finalDF.show(5)
//save it in Parquet format(the default) in directory /loudacre/webpage_files
finalDF.repartition(1).write.mode("overwrite").save("/loudacre/webpage_files")


//Using Hue or the HDFS command line tool, list the Parquet files that where saved by Spark SQL
hdfs dfs -ls /loudacre/webpage_files
//Use parquet-tools schema and parquet-tools head to review the schema and some sample data of the generated files.
hdfs dfs -get /loudacre/webpage_files/part-r-00000-1945a194-e587-4999-b7e7-b302be2a3ddf.gz.parquet /home/training/Desktop/files/webpage_result.parquet
parquet-tools schema /home/training/Desktop/files/webpage_result.parquet 
parquet-tools head /home/training/Desktop/files/webpage_result.parquet
*/