GLEON (c) 2025 JDK 21,SQLLITE DB,SPRINGBOOT 

=============


Hi, in main project folder     **/leonbookinsKarpathos**


    mvn clean install
    docker build -t leon-application .
    docker run -p 9191:9191 leon-application








### (-1st) Zero (Step - build maven project)
                mvn clean install 
### (1st) First (Step - build the image) 
###   BUILD 
                docker build -t leon-application .
### (2nd) First (Step - run it with docker)
### RUN
                docker run -p 9191:9191 leon-application


                