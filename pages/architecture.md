---
layout: page
show_meta: false
title: "Architecture"
header:
  image: 'banner.png'
  caption: 'An experimental platform for building the peer web from Microsoft'
  background-color: "#CA0046"
permalink: "/Architecture/"
---
<ul class="side-nav">
    {% for page in site.pages %}
      {% if page.categories contains 'architecture' %}
        <li><a href="{{ site.url }}{{ page.url }}">{{ page.title }}</a></li>
      {% endif %}
    {% endfor %}
</ul>
