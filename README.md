# Location Tracking

This is a simple indoor localization app, developed by Sergio Soto ([@bajosoto](https://github.com/bajosoto)) and Aniket Dhar([@AniketDhar](https://github.com/AniketDhar)) for the course [IN4254 Smart Phone Sensing](http://studiegids.tudelft.nl/a101_displayCourse.do?course_id=31681) at TU Delft University. 

![alt text](https://github.com/bajosoto/location-tracking/blob/master/screenshot.png "Interface")


## About

The purpose of the app is tracking indoor localization within a building by utilizing available WiFi access points in order to determine an initial location estimate by means of a [Bayesian Filter](https://en.wikipedia.org/wiki/Recursive_Bayesian_estimation) iterative implementation.

After getting an initial estimated location, the app further narrows the location by implementing [Particle Filtering](https://en.wikipedia.org/wiki/Particle_filter) as the user walks in different directions. It implements a step counter by combining the output of acceleration and magnetic sensors in the phone. 

A full report on the implementation can be found in the Docs folder.