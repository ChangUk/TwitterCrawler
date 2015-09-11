# TwitterCrawler
Crawling machine for gathering Twitter data

## Features
* Use Twitter4j wrapper(v4.0.4) to call Twitter REST API
* Do crawling Twitter network from a seed user's ID
* Store obtained Twitter data into a database
* Provide a sample code for crawling ego network

## Prerequisite to use
Twitter Apps are necessary to use this crawling machine. You can create your own Twitter applications at [https://apps.twitter.com/](https://apps.twitter.com/). The Twitter Apps information should be stored in a file and its format is as follows:
ConsumerKey \t ConsumerSecret \t AccessToken \t AccessSecret \n
If you add Twitter Apps as many as possible, you can avoid rate limit of API call. After making Twitter Apps file, set its path in AppManager.java file.

## Develop environment
* OS: Linux, Windows
* Language: Java 8
* IDE: Eclipse Mars
* Database support: SQLite, MariaDB