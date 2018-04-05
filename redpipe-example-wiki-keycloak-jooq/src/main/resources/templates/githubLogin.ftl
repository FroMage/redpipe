<#include "header.ftl">

<div class="row">
  <p>
    It looks like you're not logged in GitHub: if you haven't already done so,
    you first need to authorize this
    application to post gists for you. For this you must go to 
    <a href="https://github.com/settings/applications/new" target="_blank">this page</a>
    (opens in a new tab), and fill in the details as such:
  </p>
  <img style="border: 1px solid gray;" src="/wiki/app/github1.png"/>.
  <p>
    Then submit, and copy and paste the clientId and secretId from this new page:
  </p>
  <img style="border: 1px solid gray;"  src="/wiki/app/github2.png"/>.
  <p>
    Into the following form:
  </p>

  <div class="col-md-12 collapsable clearfix" id="editor">
    <form action="${context.route('WikiResource.githubLogin')}" method="post">
      <div class="form-group">
        Client Id: <input type="text" name="clientId">
        <br/>
        Client Secret: <input type="text" name="clientSecret">
      </div>
      <button type="submit" class="btn btn-primary">Login to Github</button>
    </form>
  </div>

</div>

<#include "footer.ftl">
