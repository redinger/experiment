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

## Technical Architecture

- Clojure+Noir Server Side Logic
  - Uses MongoDB Document Store for Persistence
  - Exports a Backbone.js compatible REST API
  - Exports higher-level API for autocomplete, suggests, search, etc.
  - Internal Client-Server model architecture (experiment.infra.models)
  - Supports server-generated pages for parts of the application
  - Uses handlebars-clj to define templates in server-side code that
    can be used by the server or sent to the client
- Coffeescript+Backbone Rich-Client Front-end
  - Uses d3.js for data visualizations (see QIchart.js library)
- Leverages Twitter Bootstrap for UI Elements

## Detailed Documentation

See the literate-style [Marginalia
documentation](http://github.com/eslick/experiment/tree/master/docs/uberdoc.html)
of key sections of the server code base for a more detailed overview.

See NOTES.md for more information on design and roadmap.

## Usage

```bash
lein deps
lein run
```

## License

Copyright (C) 2011 Ian Eslick

Distributed under the BSD License.  See [LICENSE.md](http://github.com/eslick/experiment/tree/master/LICENSE.md)

