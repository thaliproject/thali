---
layout: page
show_meta: false
title: "Documentation"
header:
   image_fullwidth: "no-internet-no-problem.png"
permalink: "/Documentation/"
---
<ul class="side-nav">
    {% for page in site.pages %}
      {% if page.categories contains 'documentation' %}
        <li><a href="{{ site.url }}{{ page.url }}">{{ page.title }}</a></li>
      {% endif %}
    {% endfor %}
</ul>
