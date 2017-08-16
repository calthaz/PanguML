#!"C:\ProgramData\Anaconda3\envs\python2\python.exe"
import cgi
import cgitb; cgitb.enable()  # for troubleshooting
import cv2

print "Content-type: text/html"
print

print """
<html>

<head><title>Sample CGI Script</title></head>

<body>

  <h3> Sample CGI Script </h3>
"""

form = cgi.FieldStorage()
message = form.getvalue("message", "(no message)")

print """
<p>D:\Python\Python35\python.exe</p>
<p>C:\ProgramData\Anaconda3\envs\python2\python.exe</p>
  <p>Previous message: %s</p>

  <p>form

  <form method="get" action="dummy.cgi">
    <p>message: <input type="text" name="message"/></p>
    <p>message: <input type="text" name="me23ssage"/></p>
    <p>message: <input type="text" name="messa5ge"/></p>
    <p>message: <input type="text" name="messawege"/></p>
    <p>message: <input type="text" name="mressagge"/></p>
  </form>

"""%message
print cv2.__version__
print "<h2>"+__name__+"</h2>"
print """
</body>
</html>
"""