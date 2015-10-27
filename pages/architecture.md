---
layout: page
show_meta: false
title: "Architecture"
header:
   image_fullwidth: "NoInternetNoProblem.png"
permalink: "/Architecture/"
---
<ul>
    {% for page in site.pages %}
      {% if page.categories contains 'architecture' %}
        <li><a href="{{ site.url }}{{ page.url }}">{{ page.title }}</a></li>
      {% endif %}
    {% endfor %}
</ul>
