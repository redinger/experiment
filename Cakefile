# Module requires
{spawn, exec} = require 'child_process'
sys = require 'sys'

# ## Helpers

# Helper function for showing error messages if anyting happens
printOutput = (process) ->
  process.stdout.on 'data', (data) -> sys.print data
  process.stderr.on 'data', (data) -> sys.print data

# Watch Javascript for changes
watchJS = ->
  coffee = exec 'coffee -cw -o ./resources/public/js coffee/'
  printOutput(coffee)

# Watch CSS for changes
watchCSS = ->
  stylus = exec 'stylus ./stylus/* --watch --firebug --out ./resources/public/css'
  printOutput(stylus)

# ## Tasks
# I guess pretty self explainory? lol
task 'watch', 'Watches all Coffeescript(JS) and Stylus(CSS) files', ->
  watchJS()
  watchCSS()

task 'watchJS', 'Watches all coffeescript files for changes', ->
  watchJS()

task 'watchCSS', 'Watches all CSS files for changes', ->
  watchCSS()

task 'compileJS', 'Compiles all Coffeescript files into JS', ->
 coffee = exec "coffee -cj -o ./resources/public/js coffee/" # coffee/models/ coffee/views/"
 printOutput(coffee)

#task 'test', 'Runs all tests', ->
#  vows = exec 'vows test/*.test.js'
#  printOutput(vows)

task 'docs', 'Create documentation using Docco', ->
  docco = exec """
    docco coffee/*.coffee
    docco coffee/views/*.coffee
    docco coffee/models/*.coffee
    docco test/*.coffee
  """
  printOutput(docco)