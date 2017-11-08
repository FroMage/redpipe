<#include "header.ftl">

<div class="row">

  <div class="col-md-12 mt-1">
  <#if context.canCreatePage>
    <div class="float-xs-right">
      <form class="form-inline" action="${context.uriInfo.getBaseUriBuilder().path(context.WikiResource).path(context.WikiResource, 'create').build()}" method="post">
        <div class="form-group">
          <input type="text" class="form-control" id="name" name="name" placeholder="New page name">
        </div>
        <button type="submit" class="btn btn-primary">Create</button>
      </form>
    </div>
  </#if>
    <h1 class="display-4">${context.title}</h1>
    <div class="float-xs-right">
      <a class="btn btn-outline-danger" href="${context.uriInfo.getBaseUriBuilder().path(context.SecurityResource).path(context.SecurityResource, 'logout').build()}" role="button" aria-pressed="true">Logout (${context.username})</a>
    </div>
  </div>

  <div class="col-md-12 mt-1">
  <#list context.pages>
    <h2>Pages:</h2>
    <ul>
      <#items as page>
        <li><a href="${context.uriInfo.getBaseUriBuilder().path(context.WikiResource).path(context.WikiResource, 'renderPage').build(page)}">${page}</a></li>
      </#items>
    </ul>
  <#else>
    <p>The wiki is currently empty!</p>
  </#list>
  <#if context.canCreatePage>
  <#if context.backup_gist_url?has_content>
    <div class="alert alert-success" role="alert">
      Successfully created a backup:
      <a href="${context.backup_gist_url}" class="alert-link">${context.backup_gist_url}</a>
    </div>
  <#else>
    <p>
      <form action="${context.uriInfo.getBaseUriBuilder().path(context.WikiResource).path(context.WikiResource, 'backup').build()}" method="post">
        <button type="submit" class="btn btn-outline-secondary btn-sm">Backup</button>
      </form>
    </p>
  </#if>
  </#if>
  </div>

</div>

<#include "footer.ftl">
