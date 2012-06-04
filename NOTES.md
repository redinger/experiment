# The Experiment Project -- Design Notes

## Platform Architecture

This is a rich-client style architecture.  The main site consists of a
set of tabs, each of which is a single-page app that users Backbone.js
and the history object to maintain the impression of a normal flow of
sub-page urls within the application.  Accordingly, the server handles
data processing and the client handles visualization and editing of
models, as well as asking the server to perform pre-processing of data
queries that are visualized on the front-end.

Basic support is provided for dispatching on the user-agent or a
cookie value for different web-based clients.  A mobile app client can
use the APIs directly and bypass the /app namespace.

A client app consists of:

- HTML skeleton for site frame and place holder divs for app modules
- Application javascript files
- Model templates for dynamic rendering
- Boostrap data for the specific sub-view being requested [This means the server needs to have a way of determining what data is being rendered and some duplication of URL parsing between server and client]

The server exposes various APIs to clients:

- /api/root/...  - A backbone API to enable fetching and updating of models
- /api/embed/...  - A submodel backbone extension API for embedded models
- /api/suggest/... - A set of autoSuggest AJAX APIs for helping the UI
- /api/chart/... - Renders chart content for various model / data types
- /api/events/upcoming - Renders a calendar of reminders or past events
- /api/events/calendar - Renders a calendar of reminders or past events

## Technical Architecture

- Clojure+Noir Server Side Logic
  - Uses MongoDB Document Store for Persistence
  - Exports a Backbone.js compatible REST API (+ Backbone.Embedded extension)
  - Exports higher-level API for autocomplete, suggests, search, etc.
  - Internal Client-Server model architecture (experiment.infra.models)
  - Supports server-generated pages for parts of the application
  - Uses handlebars-clj to define templates in server-side code that
    can be used by the server or sent to the client
- Coffeescript+Backbone Rich-Client Front-end
  - Uses d3.js for data visualizations (see QIchart.js library)
- Leverages Twitter Bootstrap for UI Elements

## Data Model

The server manages and provides core and function-specific APIs for a rich-client
model and some of the supplemental page logic.  

- User

### Schematic layer

- Instrument - Definition a variable and a means of measurement
- Treatment - Definition of a concrete intervention
- Experiment 
  - a Treatment
  - outcome Instruments
  - covariate Instruments
  - Schedule template

### Operative layer

- Trial
  - Experiment
  - Schedule (for reminders and treatment periods)

- Trackers
  - Tracking Schedule (for manual instruments)

## Tracking Workflow

A study has a schedule (one of a number of templates). A dispatch
function against the schedule type can transform a schedule into
reminders or serve as the backdrop for user-generated events.

- Generates reminders
   - Reminders can be put on a calendar

- User PROs are stored as time series in 'trackers'
   - Background API dumps generated events (dep on instrument)
   - Tracker objects record a series of data points from an instrument
   - Trackers can also be extracted to show recording events on a calendar
   - Trackers are processed to create canned reports or charts for a time period

# Releases

## Alpha v0.1 - Academy Health Prototype Release (November 15th, 2011)

- Basic show and tell: http://youtu.be/_OLqJtqzvK4

## Alpha v0.2 - Authoring Study Prototype Release (March 16th, 2012)

- Switched to use Twitter Bootstrap UI
- Dropped demo Dashboard and navigation features
- Added Authoring Study under Research Tab

## v0.9 - Beta Release (June 4th, 2012)

Architectural design tasks

   - X Model abstraction on server
     - X Backbone-based abstraction layer (base classes, protocols) for the 'model model'
     - X Handle references
     - X Nested object REST API support for Backbone
     - - Bootstrapping data into views (transmit entire DB for phase I)
   - X Auto-complete for tags, model names, etc.
   - X Structured treatment descriptions using auto-complete
   - X Site navigation model using dispatch
   - X Proper support for dev/prod mode and versioning
   - X Break coffeescript into multiple independent UI modules
   - X Support proper logging through server and client side
   - X Google analytics integration

   - X Release model for aggregated/versioned javascript files
   - X Dynamic loading, caching, and pre-rendering Templates, etc.
   - X Model abstraction on server
      - X Handle synchronization conflicts for models
      - X Define important properties like type checks, etc
   - X Implement server-side filtering of data and actions, how to
     communicate ACLs to client?
   - X Dump and reload database
   - X Database and server backup

Feature tasks

   - X Study registration
   - X Editorial content
   - X Refactor Trial UI into components
   - X Tracker backend services
   - X Profile editing / forms
     - X Pretty forms CSS or JS
   - X Dashboard main page
     X Outcome control chart
     - X Illustrate missing data chart
     - X Trial schedule and view
     - X Date range selection for tracker page / trial view
     - X Calendar click support
     - X I want to click on tracking to change data
   - Explore Page
     - X Style search object views
     - X Start trial from experiment view
       - X Handle schedule and reminders 
     - X Object create / edit screens


## Tasks for v1.0 - Study 2 Release

Study 2 Support

   - Study 2 page
   - Study 2 content
   - Run experiments
   - Study 2 QA


## Tasks for v1.1 - Production Release

Architecture design tasks

   - Site is down page on restarts, etc.
   - NO IE popup on login
   - ~ Error handling across server & client

Feature tasks

   - Full UX and UI review   
   - Social components


# Design Notes
# --------------------------------------------------------------

## Subsystems

### Implicit Infrastructure

 -  Site-Properties - Configuration file for sensitive configuration data
 -  Middleware 
    -  Session user - Establishes (session/current-user) from DB for each request
 -  Models - Generic way of dealing with client/server models
 -  Data API - Support Backbone + Backbone.Embedded models
 -  Service APIs - Search, Events, Event Calendar, Open mHealth, oauth connections
 -  Services - Generic way to define, configure and support connecting to 3rd party services
 -  Dynamic Handlebar Templates - Generate server-side templates using Hiccup, dynamic loader
 -  Scheduler Controller - Schedule and manage events

### Application Infrastructure

 -  QI Charts - Library for charting
 -  Schedule and Events - Data model for defining and managing events
 -  SMS Subsystem - Support SMS gateway connectivity and result sorting/parsing
 -  E-mail Subsystem (Partial) - Support user communications
    - as instrument?
 -  Twitter - TW connect, Post updates, parse health updates?, as instrument?
 -  Facebook - FB connect, Post updates, as instrument?
 -  Social Integration - Integrate with social networks (TBD) 


# Open Platform Issues
# ----------------------------------------------------------

## Longer term architectural issues

   - X Server-side dispatch to client 'views' based on user-agent.  E.g. iphone
     sends mobile client-app, search engine gets simple HTML view, web clients
     get web app and older browsers get 'install new browser' page.

   - Support search indexibility and SEO by rendering sub-views with
     proper links on the server side for search crawlers or
     accessibility.  The server-side of a given application will have
     to handle this.  (For example, we want nice SEO URLs for all
     browseable objects such as treatments, experiments, and public
     discussions.

   - ? Pallet platform distribution model
      - pallet, git, keys
      ~ Mongo installation, nginx as proxy and static files
      - TODO: Leinengin
      - TODO: Site upgrade scripts


