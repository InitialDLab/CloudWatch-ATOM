# CloudWatch-ATOM

Author: Min Du, Feifei Li
Email: min.du.email@gmail.com, lifeifei@cs.utah.edu

Demonstrate the idea of ATOM project to extend CloudWatch for tracking and monitoring resource usage data.

In original CloudWatch, each Node Controller sends resource usage data to Cloud Controller every minute, and Cloud Controller monitors these data using simple threshold approach. ATOM extends this process by: 1) using the optimal online tracking algorithm to selectively send desired values with more fine-grained granularity; 2) using PCA to automatically detect anomaly from the reported data; 3) we are also able to dynamically set the tracking threshold based on the PCA detection results. Details could be found in our paper below.

See our web demo:
  * [ATOM Demo](http://mpserv4.cs.utah.edu:8000/)


## Publications

If you are using this idea or source code for your papers or for your work, please cite the paper:

[ATOM: Automated Tracking, Orchestration, and Monitoring of Resource Usage in Infrastructure as a Service Systems](http://www.cs.utah.edu/~lifeifei/papers/atom.pdf), Min Du, Feifei Li, In Proceedings of 2015 IEEE International Conference on Big Data	(IEEE BigData 2015), pages TBA, Santa Clara, CA, November 2015.

## Dependencies

+ Java (+1.6)

