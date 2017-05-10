# Matplotlib In OSX and VirtualEnvs

http://stackoverflow.com/questions/21784641/installation-issue-with-matplotlib-python

```
RuntimeError: Python is not installed as a framework. The Mac OS X backend will not be able to function correctly if Python is not installed as a framework. See the Python documentation for more information on installing Python as a framework on Mac OS X. Please either reinstall Python as a framework, or try one of the other backends. If you are Working with Matplotlib in a virtual enviroment see 'Working with Matplotlib in Virtual environments' in the Matplotlib FAQ
```

Solution:

```
import matplotlib.pyplot as plt

plt.use('TkAgg')
```