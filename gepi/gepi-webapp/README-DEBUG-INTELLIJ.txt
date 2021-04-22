When debugging does not work because breakpoints won't stop program execution, try the following:

1. add VM args in Jetty Runner
   -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044
2. launch application and then Attach Debugger

(source: https://github.com/guikeller/jetty-runner/issues/70#issuecomment-612880966)