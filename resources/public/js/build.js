({
	baseUrl: "./",
	namespace: 'intldr',
	paths: {use: 'libs/require/use.min',
			require: "libs/require/require",
			jquery: "empty:", 
			jqueryTimeAgo: 'libs/jquery/jquery.timeago',
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
           "jqueryTimeAgo": { deps: ["jquery"] },
		   "jQueryUI": { deps: ["jquery"] },
		   "jQuerySuggest": { deps: ["use!jQueryUI"] },
		   "jQuerySparkline": { deps: ["use!jQueryUI"] },
		   "Bootstrap": { deps: ["jquery"] },
		   "Backbone": { deps: ["jquery", "use!Underscore"],
						 attach: "Backbone"},
		   "BackboneForms": { deps: ["use!Backbone"] },
		   "BackboneFormsBS": { deps: ["use!BackboneForms", "use!Bootstrap"] },
		   "BackboneFormsEditors": { deps: ["use!BackboneForms", "use!jQueryUI"] },
		   "D3": { deps: ["jquery"], 
				   attach: "d3"
				 },
		   "D3time": { deps: ["use!D3"] }
		 },
	modules: [
		{
			name: "views/dialog"
		},
		{
			name: "views/settings",
			exclude: ["use!BackboneForms", "use!BackboneFormsEditors"]
		}
//		{
//			name: "prefs",
//			exclude: "views/dialog"
//		},
//		{
//			name: "views/dashboard",
//			exclude: "views/dialog"
//		},
//		{
//			name: "views/explore",
//			exclude: "views/dialog"
//		}
	]})
  