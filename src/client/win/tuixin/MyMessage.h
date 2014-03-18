#pragma once
// 消息
class MyMessage
{
public:
	// 消息附件
	class Attachment {
	public:
		Attachment(const std::string& title, const std::string& type, const std::string& filename, const std::string& url);
		Attachment();

		std::string getTitle() { return title; }
		void setTitle(const std::string& title) { this->title = title; }

		std::string getType() { return type; }
		void setType(const std::string& type) { this->type = type; }

		std::string getFilename() { return filename; }
		void setFilename(const std::string& filename) { this->filename = filename; }

		std::string getUrl() { return url; }
		void setUrl(const std::string& url) { this->url = url; }
		
	private:
		std::string title; // 附件标题
		std::string type; // 附件类型
		std::string filename; // 附件文件名
		std::string url; // 附件下载URL
	};

public:
	MyMessage(const std::string& sender, const std::string& receiver, const std::string& title, const std::string& body,
		const std::string& type, const std::string& url, const std::string& generateTime, const std::string& expiration,
		const std::vector<Attachment>& attachments);
	MyMessage(const std::string& title, const std::string& body, const std::string& type, const std::string& url,
		const std::string& generateTime, const std::string& expiration, const std::vector<Attachment>& attachments);
	MyMessage(const std::string& body, const std::string& generateTime);
	MyMessage(void);
	~MyMessage(void);

private:
	std::string sender; // 消息发送者账号名称(me代表是自己发送的消息)
	std::string receiver; // 消息接收者(账号名称、电话号码或Email地址皆可，仅用于发送)
	std::string title; // 消息标题(可选)
	std::string body; // 消息内容
	std::string type; // 消息类型(tAttachmentext/xml/html)
	std::string url; // 消息详情URL(可选)
	std::string generateTime; // 消息生成时间(yyyyMMddHHmmss)
	std::string expiration; // 消息过期时间(可选，格式同上)
	std::vector<Attachment> attachments; // 消息附件
};
