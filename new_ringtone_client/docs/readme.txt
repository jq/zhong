配置new_ringtone_client

1)检出代码
	new_ringtone_client
	facebook_android
	ring
	util
	
2)配置ring
	link util代码：右键ring project->Property->java build path->Source tab->Link source
	选中util的src目录，将其链接进来
	
2配置new_ringtone_client
	a)按2)中方法，link util代码
	b）加入共享库：右键new_ringtone_client->Property->Android,在libary中分别加入facebook_android,和ring。
