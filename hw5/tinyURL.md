#  Architecture 1: TinyURL
## User Interface
![enter image description here](https://ik.imagekit.io/rylnaye185/WechatIMG3075_pdZMKpk66.png)
[UI Deisgn](https://wireframepro.mockflow.com/view/M4266b636e268b2ba07040cc571c3cc381589783685627)
## Architecture Diagram![enter image description here](https://ik.imagekit.io/rylnaye185/WechatIMG3072_ICdpLysLV.png)

## Component 1: Frontend/UI

Clients interact with the service through a web browser. They might start by visiting "tinyurl.com" and typing a long URL to create a tiny URL. The React app makes api calls to " tinyurl.com". Users may also be directed to the website after a friend shares a tiny URL (like tinyurl.com/a7k31m).

## Component 2: Scalable backend

**Language**: Java.

**Framework**: Tomcat web server.

**Deployment environment**: AWS Elastic Beanstalk, with a load balancer and auto-scaling of the Tomcat instances.

The backend is a single stateless Java application. It response to several kinds of requests. Requests for static HTML, CSS, JS, and image files are handled by bundling those files directly [in the war file (链接到外部网站。)](https://docs.oracle.com/javaee/5/tutorial/doc/bnadx.html).

**Scalablility**:
1. Different Server keeps different initial value for the tiny URL.
N servers, initial value: S1 = 1, S2 = 2, ... SN = N, Increment: N.  
e.g. 3 servers, S1 = 1,4,7... S2= 2,5,8... S3 = 3,6,9... Increment: 3
By doing this, different server will not generate the same tinyURL.
3. The tiny URL uses [0-9],  [a-z], and [A-Z] (62-positional notation) . The timeout of every <key, value> is 3600s
4. Monitor the visits and analyze to get the most frequent URLs to store in Memcache for a long time.


**API:**

 - **GET /{url}**

>request body:  
{"url": STRING}
response status: 302 OK
The implementation of this endpoint will check the database to see whether the URL is created. If it has been created, return the real URL in Memcached. Otherwise, return a notice that the URL has not been created.

 - **GET /create?url={url}&alias={alias}**

>request body:  
{"url": STRING}
response status: 200 OK
The implementation of this endpoint will check the database to see whether the URL is created. If it has been created, return the URL in Memcached. Otherwise, create a tiny URL and return it.

## Component 3: Memcached

The documents stored in our Memcached describe all the information for a given uid. Thus, the document key is the uid.

``uid -> {``
``"real_url": STRING, ``
``}``
