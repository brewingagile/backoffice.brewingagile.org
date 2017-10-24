function AccountController($scope, $resource, $routeParams) {
	var R = $resource('gui/accounts/:account', {account: $routeParams.account});
	$scope.account = R.get();
}
