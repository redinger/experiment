define(['models/infra', 'models/core', 'models/user', 'use!Backbone'],
  (Infra, Core, User) ->

# ## Service Schemas

    serviceSchemas =
      'tw',

# ## Service Model
    class Service extends Common.Model
      serviceType: 'service'

    class Services extends Common.Model
      model: Service
