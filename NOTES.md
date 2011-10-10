# The Experiment Project -- Design Notes

## Architecture

Basic idea is that the server handles data processing and the client
handles visualization and editing of models, asking the server to
perform post-processing of data returned from rich queries for
visualization on the front-end.

The rich-client app is a subsection of the site for which any URL from
the base "/app*" renders the app and the app does client-side dispatch
of the rest of the URL.

The app consists of:
   - Application javascript files
   - HTML skeleton for major sub-applications and site frame
   - Model templates for dynamic rendering
   - Boostrap data for the specific sub-view being requested
     This means the server needs to have a way of determining what
     data is being rendered.
     
## Major TODOs for initial release

   - Support logging through server and client side
   - Nested object REST API support for Backbone
   - Bootstrapping data into views
   - Auto-complete for tags, model names, etc.
   - Structured treatment descriptions using auto-complete
   - Fix Dynamic menu link generation, dispatch in View handler not dd library
   - Pallet distribution model
       - DONE: pallet, git, keys
       - PARTIAL: Mongo installation, nginx as proxy and static files
       - TODO: Leinengin

## Open Architectural Issues

   - Release model for aggregated/versioned javascript files
   - Generalized error handling across server & client
   - Model abstraction on server
     - Handle references
     - Handle synchronization conflicts for models
     - Define important properties like type checks, etc
   - Backbone-based abstraction layer (base classes, protocols) for the 'model model'
   - How to identify bootstrapped data to send for sub-URLs
   - Proper support for dev/prod mode and versioning

## Longer term architectural issues

   - Caching layer for static content or snippets?
   - Cache model templates in external files for releases
   - Support search indexibility and SEO by rendering sub-views with
     proper links on the server side for search crawlers or
     accessibility.  The server-side of a given application will have
     to handle this.  (For example, we want nice SEO URLs for all
     browseable objects such as treatments, experiments, and public
     discussions.
   - Server-side dispatch to client 'views' based on user-agent.  E.g. iphone
     sends mobile client-app, search engine gets simple HTML view, web clients
     get web app and older browsers get 'install new browser' page.
     
