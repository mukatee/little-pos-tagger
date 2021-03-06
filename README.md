# little-pos-tagger
Simple part of sentence tagger in Java, ported from a Python version explained and linked [here](http://honnibal.wordpress.com/2013/09/11/a-good-part-of-speechpos-tagger-in-about-200-lines-of-python/).

This is something I did when trying to get a better understanding of POS taggers and build one to use myself with some Java code I have.

There area also several more mature POS taggers for Java such as [OpenNLP](http://opennlp.apache.org/) and [Stanford](http://nlp.stanford.edu/software/tagger.html) versions.

This is maybe a bit more simple and has some useful explanation you can follow from the Python link above. So maybe easier to get an idea of at least the basic concepts.

I originally tried this with the Finnish language and used the [FinnTreeBank](http://www.ling.helsinki.fi/kieliteknologia/tutkimus/treebank/) data to train the tagger.
However, any language and similar datasets should probably work.

There is a Python [script](https://github.com/mukatee/little-pos-tagger/blob/master/src/main/python/ftb_parser.py) 
in the source tree that was used to parse the FinnTreeBank to suitable format for what this eats.
There is also another Python [script](https://github.com/mukatee/little-pos-tagger/blob/master/src/main/python/ftb_parser_opennlp.py) 
there that takes the same data and outputs a format suitable for OpenNLP.
You can then try the different approaches if you like. And use them as a basis for some other treebanks I guess..
I couldn't quite figure out a good configuration for the Stanford tagger but it should be able to take one of the above inputs as well if you can create the config.

There is are examples in the [examples](https://github.com/mukatee/little-pos-tagger/tree/master/src/main/java/net/kanstren/littlepos/examples) package 
on how to [train](https://github.com/mukatee/little-pos-tagger/blob/master/src/main/java/net/kanstren/littlepos/examples/FTBLoader.java) the tagger and 
how to use it for [predictions](https://github.com/mukatee/little-pos-tagger/blob/master/src/main/java/net/kanstren/littlepos/examples/PredictionTester.java).

The process of building this and trying to figure out what it is all about is explained in more detail [here](https://swenotes.wordpress.com/2017/03/06/building-a-finnish-part-of-speech-tagger/).