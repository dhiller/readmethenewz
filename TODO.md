Playback
========
  - (done) Improve abbreviation replacements by adding dot instead of space character
    - (optional) improve replacement by excluding sequences with ending s i.e. ISPs => I.S.Ps
  - (done) Add source before title (i.e. "slashdot: here goes the title")
  - (done) Replace single quoted terms with double quotes
  - (done) Replace hyphens between words with space
  - (done) Add pauses
    - issue after headline
    - issue after article
  - (optional) When playback is german, english words are uttered with german pronunciation, see [article](https://code.google.com/p/language-detection/)

Control
=======
  - Enable playback volume control using volume buttons
  - (done) Add media button support
  - Implement support for headset plug events
  - (optional) add bluetooth controls
  - (optional) voice control

Notification
============
  - Add notification with play/pause button

Feed import
===========
  - To support feedly data export, add [OPML import](http://feedly.com/#opml)

Feed support
============
  - Atom
  - Blog (?)
  
UI improvements
===============
  - Add a menu when Menu button is pressed (e.g. to terminate the app)
  - Add article link

General
=======
  - Remember Last read item per feed
  - (optional) Article followup support (for shortened articles)
  - (optional) Replace own parser with [XmlPullParser](http://developer.android.com/reference/org/xmlpull/v1/XmlPullParser.html)