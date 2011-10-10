# The Experiment Project

This is an open-source foundation of a collaborative platform for
aggregating self-experiments across large populations.  It will serve
as a test platform for collaborative creation and editing of
experiments, collaborative reviews, and discussion of experiment
execution (see also [Quantified Self](http://quantifiedself.org)).  It
may also serve as an evaluation platform for an experiment/treatment
recommendation engine and features of the
[C3N Project Portal](http://c3nproject.org), a platform supporting
clinical N:1 experiments.

Noir/Compojure/Ring on the server; basic static content + REST API for
a Backbone/jQuery/Coffeescript rich client in the browser.
Architecture intended to support custom clients (e.g. Native iPhone
apps) on mobile in the future.  

See NOTES.md for more information on design and roadmap.

## Usage

```bash
lein deps
lein run
```

## License

Copyright (C) 2011 Ian Eslick

Distributed under the BSD License.  See LICENSE.md

