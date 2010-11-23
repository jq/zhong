

(function($) {
	$(function() {
		$("#search").submit(function(event, info) {
			var div_search_result = $("#search_result");
			jQT.goTo(div_search_result);
			var text = $("input[id=search-text]", this).attr("value");
//			text.blur();
			var results = $("#results", div_search_result).empty();
			var load_more = "Tap to load more";
			var url = "/ringtoneserver/search?q="+text;

			$(".loader", div_search_result).bind("ajaxSend", function(){
				var length = $(".store", div_search_result).length;
				if (length == 0) {
					$("ul" ,div_search_result).hide();
					$(this).show();
				}
			}).bind("ajaxComplete", function(){
				$("ul" ,div_search_result).show();
				$(this).hide();
			});
			
			$.getJSON(url, 
				function(data) {
				if (data.length == 0) {
					window.alert("No result");
					return;
				}
				$.each(data, function(i, item) {
					var li = jsonToListItem(item);
					li.appendTo(results);
				});
				

				
				$("<li id=\"load_more\">").text(load_more).appendTo(results);
				$("#load_more", div_search_result).click(function() {
//					jQT.goTo("#categories", 'slide');
					append_more(div_search_result, url);
				});
				$("#load_more", div_search_result).bind("ajaxSend", function(){
					$(this).text("Loading...");
				 }).bind("ajaxComplete", function(){
					 $(this).text("Tap to load more");
					 $(this).appendTo(results);
				 });
				


			});
			

			return false;
		});
	});
})(jQuery);

//$(function(){
//    $(".song_item").click( function(e) {
//    	var link = $(this).attr("href");
//    	//window.location = link;
//    	window.open(link);
//    });
//});



function fill_cate_page(cate) {
	var div_cate = $("#category");
	$("h1", div_cate).text(cate);
	var results = $("#results", div_cate);
	results.empty();
	var url = "/ringtoneserver/search?q="+cate+"&type=category";
	
	$(".loader", div_cate).bind("ajaxSend", function(){
		var length = $(".store", div_cate).length;
		if (length == 0) {
			$("ul" ,div_cate).hide();
			$(this).show();
		}
	}).bind("ajaxComplete", function(){
		$(this).hide();
		$("ul" ,div_cate).show();
	});
	
	$.getJSON(url, 
			function(data) {
		if (data.length == 0) {
			window.alert("No result");
			return;
		}
			$.each(data, function(i, item) {
				var li = jsonToListItem(item);
				li.appendTo(results);
			});
			var load_more = "Tap to load more";
			$("<li id=\"load_more\">").text(load_more).appendTo(results);
			$("#load_more", div_cate).click(function() {
				append_more(div_cate, url);
			});
			$("#load_more", div_cate).bind("ajaxSend", function(){
				$(this).text("Loading...");
			 }).bind("ajaxComplete", function(){
				 $(this).text("Tap to load more");
				 $(this).appendTo(results);
				 setHeight($(this));
			 });
		});
};

//$(document).ready(function(e){
//        	var all_categories = $("#all_categories", this).empty();
//        	var all_cate_array = [ "Christian", "Metal", "Holiday", "R&B", "World Music",
//					"Pop", "Rock", "Games", "Dance", "Rap", "Jazz", "Hip-Hop",
//					"Gospel", "TV", "Hard Rock", "Electronic", "Latin Music",
//					"Blues", "Sound Effects", "Classical", "Comedy", "Country",
//					"Movie", "Other", "Hip", "Vocal", "Folk", "Hard", "Gothic",
//					"Avantgarde", "unknown", "Acid", "Soundtrack", "Soul", 
//					"Progressive", "Acoustic", "Ska", "Booty", "Easy", "Satire",
//					"Gangsta", "Oldies", "Heavy", "Southern", "Classic", "Disco",
//					"Alt", "Reggae", "Funk" ];
//        	for (cate in all_cate_array) {
//        		var a = $("<a></a>").attr("class", "cate_link_item").attr("href", "#category").attr("value", all_cate_array[cate]).text(all_cate_array[cate]);
//        		var str = $("<li></li>").attr("class", "arrow").append(a);
//        		str.appendTo(all_categories);
//        	}
//});

$(document).ready(function(e){
    $('#top_download').bind('pageAnimationStart', function(event, info) {
        if (info.direction == 'in') {
			var results = $("#results", this).empty();
			var load_more = "Tap to load more";
			var url = "/ringtoneserver/search?type=download_count";
			
			$(".loader", this).bind("ajaxSend", function(){
				var length = $(".store", "#top_download").length;
				if (length == 0) {
					$("ul" , "#top_download").hide();
					$(this).show();
				}
			}).bind("ajaxComplete", function(){
				$("ul" , "#top_download").show();
				$(this).hide();
			});
			
			$.getJSON(url, 
				function(data) {
				if (data.length == 0) {
					window.alert("No result");
					return;
				}
				$.each(data, function(i, item) {
					var li = jsonToListItem(item);
					li.appendTo(results);
				});
				$("<li id=\"load_more\">").text(load_more).appendTo(results);
				$("#load_more", $("#top_download")).click(function() {
					append_more($("#top_download"), url);
				});
				$("#load_more", $("#top_download")).bind("ajaxSend", function(){
					$(this).text("Loading...");
				 }).bind("ajaxComplete", function(){
					$(this).text("Tap to load more");
					$(this).appendTo(results);
				 });
			});
        }
    })
});


function append_more(parent, url) {
	var results = $("#results", parent);
	var loading = $("#load_more", parent);
	var start = $(".store", parent).length;
	$.getJSON(url+"&start="+start,
		function(data) {
		$.each(data, function(i, item) {
			var str = jsonToListItem(item);
			str.appendTo(results);
		});
	});
};

function jsonToListItem(item) {
	var arrow = $("<span></span>").attr("class", "arrow");
	var artist = $("<span></span>").attr("class", "artist").attr("value", item.artist).text(item.artist);
	var title = $("<span></span>").attr("class", "title").attr("value", item.title).text(item.title);
	var rate;
	if (item.avg_rate == 0) {
		rate = $("<span></span>").attr("class", "stars0");
	} else if (item.avg_rate <= 20) {
		rate = $("<span></span>").attr("class", "stars1");
	} else if (item.avg_rate <= 40) {
	 	rate = $("<span></span>").attr("class", "stars2");
	} else if (item.avg_rate <= 60) {
		rate = $("<span></span>").attr("class", "stars3");
	} else if (item.avg_rate <= 90) {
		rate = $("<span></span>").attr("class", "stars4");
	} else if (item.avg_rate <= 100) {
		rate = $("<span></span>").attr("class", "stars5");
	}
	var download_count = $("<span></span>").attr("target", "_blank").attr("class", "download_count").attr("value", item.download_count).text(item.down);
//	var image = $("<span></span>").attr("class", "image").attr("style", "background-image: url('"+item.image+"'); background-position:center; background-repeat:no-repeat;").attr("value", item.image);
	var image = $("<img></img>").attr("src", item.image);
	var a = $("<a></a>").attr("href", item.s3url).attr("target", "_blank").attr("class", "song_item").attr("song_id", item.uuid);
	var li = $("<li></li>").attr("class", "store");
	var div_a = a.append(image).append(artist).append(title).append(rate).append(arrow);
	return li.append(div_a);
};

