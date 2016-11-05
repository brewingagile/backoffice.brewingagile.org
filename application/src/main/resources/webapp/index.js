var app = angular.module('backoffice', ['ngResource', 'ngRoute', 'directives', 'filters', 'services'],
	function($routeProvider, $locationProvider) {
		var nocache = "?t=" + Date.now();
		$routeProvider.when('/', { redirectTo: '/received' });
		$routeProvider.when('/received', { templateUrl: 'views/received.html'+nocache, controller: ReceivedController });
		$routeProvider.when('/invoicing', { templateUrl: 'views/invoicing.html'+nocache, controller: InvoicingController });
		$routeProvider.when('/paid', { templateUrl: 'views/paid.html'+nocache, controller: PaidController });
		$routeProvider.when('/buckets', { templateUrl: 'views/buckets.html'+nocache, controller: BucketsController });
		$routeProvider.when('/tickets', { templateUrl: 'views/tickets.html'+nocache, controller: TicketsController });
		$routeProvider.when('/overview', { templateUrl: 'views/overview.html'+nocache, controller: OverviewController });
		$routeProvider.when('/registrations/:registrationId', { templateUrl: 'views/registration.html'+nocache, controller: RegistrationController });
		$routeProvider.when('/budget', {templateUrl: 'views/budget.html'+nocache, controller: BudgetController });
	}
);

app.config(function($httpProvider) {
	$httpProvider.responseInterceptors.push(function($q, $window) {
		return function(promise) {
			return promise.then(function(response) {
				return response;
			}, function(response) {
				if (response.status == 403) {
					var path = $window.location.pathname;
					var hash = $window.location.hash;
					var contextRoot = path.substring(0, path.lastIndexOf("/"));
					$window.location = contextRoot + '/login.html#?forbidden=true&hash='+encodeURIComponent(hash);
				}
				return $q.reject(response);
			});
		}
	});
});

app.directive('nulltoemptystring', function() {
    return {
        require: 'ngModel',
        link: function(scope, element, attrs, ngModel) {
            ngModel.$parsers.push(function(value) {
                if ( value === null ) return '';
                return value;
            });
        }
    };
});

app.filter('empty', function() {
	return function(s, emptyStr) {
                if (!s) return emptyStr;
                if (s === "") return emptyStr;
                return s;
	};
});

directives.directive('confirmClick', [
  function(){
    return {
      priority: 1,
      link: function(scope, element, attr){
        var msg = attr.confirmClick;
        var clickAction = attr.confirmedClick;
        element.bind('click', function(event){
          if (window.confirm(msg)) {
            scope.$eval(clickAction);
          }
        });
      }
    };
  }
]);

app.factory("RegistrationsByState", function ($resource) {
    return $resource('gui/registrations', {});
});


function BorderController($scope, $location, AuthService, VersionNumber) {
	$scope.loggedInAs = AuthService.get();
	$scope.version = VersionNumber;

	$scope.navClass = function(page) {
		return ($location.path() == '/' + page + '/') ? "active" : "";
	};
}

BorderController.$inject = ['$scope', '$location', 'AuthService', 'VersionNumber'];
