// Author: Ian Eslick
// Filename: load.js

// Require.js configuration file to load scripts

require.config(
    {
		waitSeconds: 15,
		paths: {use: 'libs/require/use.min',
				jquery: '//ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min',
				jQueryUI: 'libs/jquery/jquery-ui-1.8.18.custom.min',
				jQuerySparkline: 'libs/jquery/jquery.sparkline.min',
				jQuerySuggest: 'libs/jquery/jquery.autoSuggest.packed',
				Handlebars: 'libs/misc/handlebars.1.0.0.beta.3',
				Underscore: 'libs/underscore/underscore-min-131',
				Bootstrap: 'libs/bootstrap/bootstrap.min',
				Backbone: 'libs/backbone/backbone-min',
				BackboneRelational: 'libs/backbone/backbone-relational',
				BackboneForms: 'libs/backbone/backbone-forms',
				BackboneFormsBS: 'libs/backbone/backbone-forms-bootstrap',
				BackboneFormsEditors: 'libs/backbone/jquery-ui-editors',
				D3: "libs/d3/d3.min",
				D3time: "libs/d3/d3.time.min",
				QIchart: 'libs/qi-chart',
				Dialog: 'views/dialog'
			   },
		use: { "Underscore": { attach: "_" },
			   "Handlebars": { attach: "Handlebars" },
			   "jQueryUI": { deps: ["jquery"] },
			   "jQuerySuggest": { deps: ["use!jQueryUI"] },
			   "jQuerySparkline": { deps: ["use!jQueryUI"] },
			   "Bootstrap": { deps: ["jquery"] },
			   "Backbone": { deps: ["jquery", "use!Underscore"],
							 attach: "Backbone"},
			   "BackboneRelational": { deps: ["use!Backbone"] },
			   "BackboneForms": { deps: ["use!Backbone"] },
			   "BackboneFormsBS": { deps: ["use!BackboneForms", "use!Bootstrap"] },
			   "BackboneFormsEditors": { deps: ["use!BackboneForms", "use!jQueryUI"] },
			   "D3": { deps: ["jquery"], 
					   attach: "d3"
					 },
			   "D3time": { deps: ["use!D3"] }
			 },
		deps: ['views/dialog'],
		callback: function () {
			console.log('loaded libs + view/dialog');
		}
	})

