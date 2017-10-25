function AccountController($scope, $resource, $routeParams) {
	var R = $resource('gui/accounts/:account', {account: $routeParams.account});
	var Invoice = $resource('gui/accounts/:account/invoice', {account: $routeParams.account});
	$scope.account = R.get();

	$scope.invoice = function() {
	    $scope.invoiceAlert = null;
	    Invoice.save({}, function(d) { $scope.invoiceAlert = d; });
	};
}
