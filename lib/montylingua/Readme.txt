# MONTY LINGUA - An end-to-end natural language processor
#                for English, for the Python/Java platform
# 
# Author: Hugo Liu <hugo@media.mit.edu>
# Project Page: <http://web.media.mit.edu/~hugo/montylingua>
# 
# Copyright (c) 2002,2003 by Hugo Liu, MIT Media Lab
#  All rights reserved.
#
# Non-commercial use is free, as provided in the GNU GPL
# Please read additional copyright and licensing information in
#  "license.txt", included in this distribution
#
#
#  ABOUT:
#
#   About MontyLingua:
#   - Preprocessor
#     - Tokenizer, normalize contractions
#   - MontyTagger
#     - Part-of-speech tagging using PENN TREEBANK tagset
#     - uses and performs at levels of Brill94 tbl tagger
#   - MontyChunker
#     - chunks tagged text into verb and noun chunks
#   - MontyExtractor
#     - extracts verb-argument structures, phrases, and
#       other semantically valuable information from sentences
#   - MontyLemmatiser
#     - part-of-speech sensitive lemmatisation
#     - includes regexps from Humphreys and Carroll's morph.lex
#     - verification against the XTAG database
#
#  WHERE MUST THE DATAFILES BE?
#  - the "datafiles" include "CONTEXTUALRULEFILE", "LEXICALRULEFILE",
#    "LEXICON", "FASTLEXICON.*", and "chunkrules.txt"
#  - the best solution is to create an environment variable called
#    "MONTYLINGUA" and put the path to the datafiles there
#  - alternatively, MontyLingua can find the datafiles if they are 
#    in the operating system "PATH" variable, or in the current
#    working directory
#    
#   RUNNING and API:
#   
#     MontyLingua can be called from Python, Java, or run at the
#     command line.
#     
#     A. From Python, look in MontyLingua.py for the API
#     B. From your Java code:
#       1. make sure "montylingua.jar" is
#         in your class path, in addition to
#         associated subdirectories and data files
#       2. in your code, you need something like:
#
#       import montylingua.JMontyLingua; // loads namespace
#       public class YourClassHere {
#         public static JMontyLingua j = new JMontyLingua();
#         public yourFunction(String raw, String toked) {
#            jisted = j.jist_predicates(raw); // an example function
#
#       3. For a good use case example, see Sample.java.
#       4. For the MontyLingua JAVA API, see JAVA_API.txt
#     C. From the command line:
#          type "runJavaCommandline.bat"
#           (make sure java is in your path)
# 
#  VERSION HISTORY:
#
#   Bugfixed version 1.3.1 (11 Nov 2003)
#     - datafiles can now sit in the current working directory (".")
#       or in the path of either of the two environment variables 
#       "MONTYLINGUA" or "PATH" 
# 
#   New in Version 1.3 (5 Nov 2003)
#     - lisp-style predicate output added
#     - Sample.java example file added to illustrate API
#   
#   New in Version 1.2 (12 Sep 2003)
#     - MontyChunker rules expanded
#     - MontyLingua JAVA API added
#     - MontyLingua documentation added
#
#   New in Version 1.1 (1 Sep 2003)
#     - MontyTagger optimized, 2X loading and 2.5X tagging speed
#     - MontyLemmatiser added to MontyLingua suite
#     - MontyChunker added
#     - MontyLingua command-line capability added
#
#   New in Version 1.0 (3 Aug 2003)
#     - First release
#     - MontyTagger (since 15 Jan 2001) added to MontyLingua
#
# --please send bugs & suggestions to hugo@media.mit.edu--
#