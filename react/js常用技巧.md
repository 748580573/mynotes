###  JS获取元素的offsetTop,offsetLeft等属性

* obj.clientWidth //获取元素的宽度（width+padding）
* obj.clientHeight //元素的高度
* obj.offsetLeft //元素相对于父元素的left
* obj.offsetTop //元素相对于父元素的top
* obj.offsetWidth //元素的宽度(width+padding+border)
* obj.offsetHeight //元素的高度

````js
//获取元素的纵坐标（相对于窗口）
 function getTop(e){
   var offset=e.offsetTop;
   if(e.offsetParent!=null) offset+=getTop(e.offsetParent);
   return offset;
 }
//获取元素的横坐标（相对于窗口）
 function getLeft(e){
   var offset=e.offsetLeft;
   if(e.offsetParent!=null) offset+=getLeft(e.offsetParent);
   return offset;
 }
````

*上面的方法无疑影响性能，好在浏览器给我提供了相应的接口getBoundingClientRect，这个方法最早出现在IE浏览器中，后来的浏览器也跟着支持了这个方法，而且还更加完善，IE中只能获取到元素的left,top,bottom,right的属性，而后面的现代浏览器还能获取到元素的width和height.*

| **Chrome** | **Firefox (Gecko)** | **Internet Explorer** | **Opera** | **Safari** |
| ---------- | ------------------- | --------------------- | --------- | ---------- |
| *1.0*      | *3.0 (1.9)*         | *4.0*                 | *(Yes)*   | *4.0*      |

*这里要注意的是，bottom是元素底部相对于窗口顶部的距离，而不是像css里面position的bottom相对于窗口底部，同理，rihgt属性是元素最右边相对于窗口左边的距离。*

 

```javascript
 var box = document.getElementById("box");
 var pos = box.getBoundingClientRect();
 box.innerHTML = "top:"+pos.top +
   "left:"+pos.left +
   "bottom:"+pos.bottom +
   "right:"+pos.right +
   "width:"+pos.width +
   "height:"+pos.height
```