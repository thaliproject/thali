---
layout: page
show_meta: false
title: "Architecture"
header:
   image_fullwidth: "no-internet-no-problem.png"
permalink: "/Architecture/"
---
<ul class="side-nav">
    {% for page in site.pages %}
      {% if page.categories contains 'architecture' %}
        <li><a href="{{ site.url }}{{ page.url }}">{{ page.title }}</a></li>
      {% endif %}
    {% endfor %}
</ul>
