# The Experiment Project -- Design Notes

## Architecture

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
   - Boostrap data for the specific sub-view being requested
   - This means the server needs to have a way of determining what data is being rendered and some duplication of URL parsing between server and client
     
## Major TODOs for initial release

   x Bootstrapping data into views
   x Nested object REST API support for Backbone
   - Support proper logging through server and client side
   ~ Auto-complete for tags, model names, etc.
   - Structured treatment descriptions using auto-complete
   x Fix Dynamic menu link generation, dispatch in View handler
   - Pallet distribution model
       - DONE: pallet, git, keys
       - PARTIAL: Mongo installation, nginx as proxy and static files
       - TODO: Leinengin
       - TODO: Site upgrade scripts

## Open Architectural Issues

   - Release model for aggregated/versioned javascript files
   - Error handling across server & client
   - Model abstraction on server
     x Handle references
     - Handle synchronization conflicts for models
     - Define important properties like type checks, etc
   x Backbone-based abstraction layer (base classes, protocols) for the 'model model'
   x How to identify bootstrapped data to send for sub-URLs
   - Proper support for dev/prod mode and versioning

## Longer term architectural issues

   - Caching layer for static content or snippets?
   - Cache model templates in external files for release
   - Support search indexibility and SEO by rendering sub-views with
     proper links on the server side for search crawlers or
     accessibility.  The server-side of a given application will have
     to handle this.  (For example, we want nice SEO URLs for all
     browseable objects such as treatments, experiments, and public
     discussions.
   - Server-side dispatch to client 'views' based on user-agent.  E.g. iphone
     sends mobile client-app, search engine gets simple HTML view, web clients
     get web app and older browsers get 'install new browser' page.
     
## Feature Wishlist

   - Site introduction similar to Coda's: http://www.panic.com/coda/
