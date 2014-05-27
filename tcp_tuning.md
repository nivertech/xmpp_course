
Set up port forwarding
----------------------

Xitrum listens on port 8000 and 4430 by default.
You can change these ports in ``config/xitrum.conf``.

You can update ``/etc/sysconfig/iptables`` with these commands to forward port
80 to 8000 and 443 to 4430:

    sudo su - root
    chmod 700 /etc/sysconfig/iptables
    iptables-restore < /etc/sysconfig/iptables
    iptables -A PREROUTING -t nat -i eth0 -p tcp --dport 80 -j REDIRECT --to-port 8000
    iptables -A PREROUTING -t nat -i eth0 -p tcp --dport 443 -j REDIRECT --to-port 4430
    iptables -t nat -I OUTPUT -p tcp -d 127.0.0.1 --dport 80 -j REDIRECT --to-ports 8000
    iptables -t nat -I OUTPUT -p tcp -d 127.0.0.1 --dport 443 -j REDIRECT --to-ports 4430
    iptables-save -c > /etc/sysconfig/iptables
    chmod 644 /etc/sysconfig/iptables


Of course for example if you have Apache running on port 80 and 443, you have to stop it:

    sudo /etc/init.d/httpd stop
    sudo chkconfig httpd off


Good read:

* `Iptables tutorial <http://www.frozentux.net/iptables-tutorial/chunkyhtml/>`_

Tune Linux for massive connections
----------------------------------

Note that on Mac, `JDKs suffer from a serious problem with IO (NIO) performance <https://groups.google.com/forum/#!topic/spray-user/S-SNR2m0BWU>`_.

Good read:

* `Linux Performance Tuning (Riak) <http://docs.basho.com/riak/latest/ops/tuning/linux/>`_
* `AWS Performance Tuning (Riak) <http://docs.basho.com/riak/latest/ops/tuning/aws/>`_
* `Ipsysctl tutorial <http://www.frozentux.net/ipsysctl-tutorial/chunkyhtml/>`_
* `TCP variables <http://www.frozentux.net/ipsysctl-tutorial/chunkyhtml/tcpvariables.html>`_

Increase open file limit
~~~~~~~~~~~~~~~~~~~~~~~~

Each connection is seen by Linux as an open file.
The default maximum number of open file is 1024.
To increase this limit, modify /etc/security/limits.conf:

    *  soft  nofile  1024000
    *  hard  nofile  1024000


You need to logout and login again for the above config to take effect.
To confirm, run ``ulimit -n``.

Tune kernel
~~~~~~~~~~~

As instructed in the article `A Million-user Comet Application with Mochiweb <http://www.metabrew.com/article/a-million-user-comet-application-with-mochiweb-part-1>`_,
modify /etc/sysctl.conf:


    # General gigabit tuning
    net.core.rmem_max = 16777216
    net.core.wmem_max = 16777216
    net.ipv4.tcp_rmem = 4096 87380 16777216
    net.ipv4.tcp_wmem = 4096 65536 16777216
    
    # This gives the kernel more memory for TCP
    # which you need with many (100k+) open socket connections
    net.ipv4.tcp_mem = 50576 64768 98152
    
    # Backlog
    net.core.netdev_max_backlog = 2048
    net.core.somaxconn = 1024
    net.ipv4.tcp_max_syn_backlog = 2048
    net.ipv4.tcp_syncookies = 1


Run ``sudo sysctl -p`` to apply.
No need to reboot, now your kernel should be able to handle a lot more open connections.

Note about backlog
~~~~~~~~~~~~~~~~~~

TCP does the 3-way handshake for making a connection.
When a remote client connects to the server,
it sends SYN packet, and the server OS replies with SYN-ACK packet,
then again that remote client sends ACK packet and the connection is established.
Xitrum gets the connection when it is completely established.

According to the article
`Socket backlog tuning for Apache <https://sites.google.com/site/beingroot/articles/apache/socket-backlog-tuning-for-apache>`_,
connection timeout happens because of SYN packet loss which happens because
backlog queue for the web server is filled up with connections sending SYN-ACK
to slow clients.

According to the
`FreeBSD Handbook <http://www.freebsd.org/doc/en_US.ISO8859-1/books/handbook/configtuning-kernel-limits.html>`_,
the default value of 128 is typically too low for robust handling of new
connections in a heavily loaded web server environment. For such environments,
it is recommended to increase this value to 1024 or higher.
Large listen queues also do a better job of avoiding Denial of Service (DoS) attacks.

The backlog size of Xitrum is set to 1024 (memcached also uses this value),
but you also need to tune the kernel as above.

To check the backlog config:

    cat /proc/sys/net/core/somaxconn

Or:

    sysctl net.core.somaxconn

To tune temporarily, you can do like this:

    sudo sysctl -w net.core.somaxconn=1024



HAProxy tips
------------

To config HAProxy for SockJS, see `this example <https://github.com/sockjs/sockjs-node/blob/master/examples/haproxy.cfg>`_.

To have HAProxy reload config file without restarting, see `this discussion <http://serverfault.com/questions/165883/is-there-a-way-to-add-more-backend-server-to-haproxy-without-restarting-haproxy>`_.

HAProxy is much easier to use than Nginx. It suits Xitrum because as mentioned in
:doc:`the section about caching </cache>`, Xitrum serves static files
`very fast <https://gist.github.com/3293596>`_. You don't have to use the static file
serving feature in Nginx.


Nginx tips
----------

If you use WebSocket or SockJS feature in Xitrum and want to run Xitrum behind
Nginx 1.2, you must install additional module like
`nginx_tcp_proxy_module <https://github.com/yaoweibin/nginx_tcp_proxy_module>`_.
Nginx 1.3+ supports WebSocket natively.

Nginx by default uses HTTP 1.0 protocol for reverse proxy. If your backend server
returns chunked response, you need to tell Nginx to use HTTP 1.1 like this:


    location / {
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_pass http://127.0.0.1:8000;
    }


The `documentation <http://nginx.org/en/docs/http/ngx_http_upstream_module.html#keepalive>`_ states that for http keepalive, you should also set proxy_set_header Connection "";

