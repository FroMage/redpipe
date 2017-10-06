'use strict';

angular.module("wikiApp", [])
  .controller("WikiController", ["$scope", "$http", "$timeout", function ($scope, $http, $timeout) {

    var DEFAULT_PAGENAME = "Example page";
    var DEFAULT_MARKDOWN = "# Example page\n\nSome text _here_.\n";

$scope.newPage = function() {
  $scope.pageId = undefined;
  $scope.pageName = DEFAULT_PAGENAME;
  $scope.pageMarkdown = DEFAULT_MARKDOWN;
};

$scope.reload = function () {
  $http.get("/wiki/api/pages").then(function (response) {
    $scope.pages = response.data.pages;
  });
};

$scope.pageExists = function() {
  return $scope.pageId !== undefined;
};

$scope.load = function (id) {
  $http.get("/wiki/api/pages/" + id).then(function(response) {
    var page = response.data.page;
    $scope.pageId = page.id;
    $scope.pageName = page.name;
    $scope.pageMarkdown = page.markdown;
    $scope.updateRendering(page.html);
  });
};

$scope.updateRendering = function(html) {
  document.getElementById("rendering").innerHTML = html;
};

$scope.save = function() {
  var payload;
  if ($scope.pageId === undefined) {
    payload = {
      "name": $scope.pageName,
      "markdown": $scope.pageMarkdown
    };
    $http.post("/wiki/api/pages", payload).then(function(ok) {
      $scope.reload();
      $scope.success("Page created");
      var guessMaxId = _.maxBy($scope.pages, function(page) { return page.id; });
      $scope.load(guessMaxId.id || 0);
    }, function(err) {
      $scope.error(err.data.error);
    });
  } else {
    var payload = {
      "markdown": $scope.pageMarkdown
    };
    $http.put("/wiki/api/pages/" + $scope.pageId, payload).then(function(ok) {
      $scope.success("Page saved");
    }, function(err) {
      $scope.error(err.data.error);
    });
  }
};

$scope.delete = function() {
  $http.delete("/wiki/api/pages/" + $scope.pageId).then(function(ok) {
    $scope.reload();
    $scope.newPage();
    $scope.success("Page deleted");
  }, function(err) {
    $scope.error(err.data.error);
  });
};

$scope.success = function(message) {
  $scope.alertMessage = message;
  var alert = document.getElementById("alertMessage");
  alert.classList.add("alert-success");
  alert.classList.remove("invisible");
  $timeout(function() {
    alert.classList.add("invisible");
    alert.classList.remove("alert-success");
  }, 3000);
};

$scope.error = function(message) {
  $scope.alertMessage = message;
  var alert = document.getElementById("alertMessage");
  alert.classList.add("alert-danger");
  alert.classList.remove("invisible");
  $timeout(function() {
    alert.classList.add("invisible");
    alert.classList.remove("alert-danger");
  }, 5000);
};

$scope.reload();
$scope.newPage();

var markdownRenderingPromise = null;
$scope.$watch("pageMarkdown", function(text) {
  if (markdownRenderingPromise !== null) {
    $timeout.cancel(markdownRenderingPromise);
  }
  markdownRenderingPromise = $timeout(function() {
    markdownRenderingPromise = null;
    $http.post("/wiki/app/markdown", text).then(function(response) {
          $scope.updateRendering(response.data);
    });
  }, 300);
});

}]);