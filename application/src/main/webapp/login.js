function LoginController ($scope, $location, $window, $resource) {
	var VersionNumber = $resource("gui/versionnumber");
	$scope.messages = { failure: false, logout: false, forbidden: false, hash: "%23" };
	$scope.messages = $location.search();
	$scope.version = VersionNumber.get();
	if ($scope.messages.forbidden) {
		// Got here by AJAX replied with forbidden
		$scope.redirectHash = $scope.messages.hash ? decodeURIComponent($scope.messages.hash) : "#";
	} else {
		// Got here by redirect to static resource (index.html)
		$scope.redirectHash = $window.location.hash;
	}
	$scope.goTwoStepsBack = function() {
 		history.go(-2);
	};
};

LoginController.inject = ['$scope', '$location', '$window', '$resource'];