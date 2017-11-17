<#include "header.ftl">

<div class="row">

  <div class="col-md-12 mt-1">
  <#if context.canUpdatePage>
      <span class="float-xs-right">
        <a class="btn btn-outline-primary" href="${context.route('WikiResource.index')}" role="button" aria-pressed="true">Home</a>
        <button class="btn btn-outline-warning" type="button" data-toggle="collapse"
                data-target="#editor" aria-expanded="false" aria-controls="editor">Edit</button>
      </span>
  </#if>
    <h1 class="display-4">
      <span class="text-muted">{</span>
    ${context.title}
      <span class="text-muted">}</span>
    </h1>
  </div>

  <div class="col-md-12 mt-1 clearfix">
  ${context.content}
  </div>

  <div class="col-md-12 collapsable collapse clearfix" id="editor">
    <form action="${context.route('WikiResource.save')}" method="post">
      <div class="form-group">
        <input type="hidden" name="id" value="${context.id}">
        <input type="hidden" name="title" value="${context.title}">
        <input type="hidden" name="newPage" value="${context.newPage}">
        <textarea class="form-control" id="markdown" name="markdown" rows="15">${context.rawContent}</textarea>
      </div>
      <button type="submit" class="btn btn-primary">Save</button>
    <#if context.id != -1 && context.canDeletePage>
      <button type="submit" formaction="${context.route('WikiResource.delete')}" class="btn btn-danger float-xs-right">Delete</button>
    </#if>
    </form>
  </div>

  <div class="col-md-12 mt-1">
    <hr class="mt-1">
    <p class="small">Rendered: ${context.timestamp}</p>
  </div>

</div>

<#include "footer.ftl">
