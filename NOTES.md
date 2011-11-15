# The Experiment Project -- Design Notes

## Platform Architecture

Basic partitioning is that the server handles data processing and the
client handles visualization and editing of models, asking the server
to perform pre-processing of data resulting from client queries that
can be visualized on a front-end.

The rich-client app is a subsection of the site for which any URL from
the base "/app*" renders the app and client handles the rest of the
URL.  Basic support is provided for dispatching on the user-agent or a
cookie value for different web-based clients.

A mobile app client can use the APIs directly and bypass the /app
namespace.

A client app consists of:
   - HTML skeleton for site frame and place holder divs for app modules
   - Application javascript files
   - Model templates for dynamic rendering
   - Boostrap data for the specific sub-view being requested [This means the server needs to have a way of determining what data is being rendered and some duplication of URL parsing between server and client]

The server exposes various APIs to clients:
   - /api/bone/...  - A backbone API to enable fetching and updating of models
   - /api/suggest/... - A set of autoSuggest AJAX APIs for helping the UI
   - /api/chart/... - Renders chart content for various model / data types
   - /api/events/calendar - Renders a calendar of reminders or past events

### Models

The server manages the logic and integrity of a set of models that the
UI shows to the user.  
   - User
   - Treatment
   - Instrument
   - Experiment
      - Schedule
   - Trial
   - Reminder
   - Embedded Objects for various objects
      - Journals
      - Comments

### Tracking Workflow

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

### Tasks for v0.1 - Demo Release

Architectural design tasks

   x Model abstraction on server
     x Backbone-based abstraction layer (base classes, protocols) for the 'model model'
     x Handle references
     x Bootstrapping data into views (transmit entire DB for phase I)
     x Nested object REST API support for Backbone
   x Auto-complete for tags, model names, etc.
   x Structured treatment descriptions using auto-complete
   x Site navigation model using dispatch

Feature tasks
   
   - Tracking charts
   - Outcome control chart
   - Trial schedule and view
   - Simple object views
   - Run Trial from Experiment view
   - Journaling and Comment Creation
   - Profile editing / forms
     - Pretty forms CSS or JS
   - Object creation screens
   - AH: Cover letter
   - AH: Presentation
   - AH: Screencast
   - Site auxilary content
   - Styling interior pages


### Tasks for v0.2 - Stability Release

Architecture design tasks

   - Proper support for dev/prod mode and versioning
   - Support proper logging through server and client side
   - Cache model templates in external files for release
   - Release model for aggregated/versioned javascript files
   - Error handling across server & client
   - Pallet distribution model
      x pallet, git, keys
      ~ Mongo installation, nginx as proxy and static files
      x TODO: Leinengin
      - TODO: Site upgrade scripts
   - Database and server backup

Feature tasks

   - Full UX and UI review   

### Tasks for v0.3 - Tuning Release

Architecture design tasks

   - Incremental bootstrapping of models to the server (how to diff?)
   - Model abstraction on server
      - Handle synchronization conflicts for models
      - Define important properties like type checks, etc
   - Caching layer for static content or snippets?
   - General error handling and form updating

Feature tasks

   - Failed login messages

### Tasks for v0.4 - Public Release

   - Pick a name
   - Logo
   - Final CSS design tune

# Open Platform Issues

### Longer term architectural issues

   - Support search indexibility and SEO by rendering sub-views with
     proper links on the server side for search crawlers or
     accessibility.  The server-side of a given application will have
     to handle this.  (For example, we want nice SEO URLs for all
     browseable objects such as treatments, experiments, and public
     discussions.
   x Server-side dispatch to client 'views' based on user-agent.  E.g. iphone
     sends mobile client-app, search engine gets simple HTML view, web clients
     get web app and older browsers get 'install new browser' page.
     
### Wishlist / Notes

   - Site introduction similar to Coda's: http://www.panic.com/coda/
   - I like github's look at feel!  (as you can tell)
