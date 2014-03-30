#pragma once
// ��Ϣ
class MyMessage
{
public:
	// ��Ϣ����
	class Attachment {
	public:
		Attachment(const std::string& title, const std::string& type, const std::string& filename, const std::string& url);
		Attachment();

		std::string getTitle() const { return title; }
		void setTitle(const std::string& title) { this->title = title; }

		std::string getType() const { return type; }
		void setType(const std::string& type) { this->type = type; }

		std::string getFilename() const { return filename; }
		void setFilename(const std::string& filename) { this->filename = filename; }

		std::string getUrl() { return url; } const
		void setUrl(const std::string& url) { this->url = url; }
		
	private:
		std::string title; // ��������
		std::string type; // ��������
		std::string filename; // �����ļ���
		std::string url; // ��������URL
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

	static MyMessage parse(const std::string& jsonText);
	std::string toJson();

	void setSender(const std::string sender) { this->sender = sender;  }
	std::string getSender() const { return sender; }
	void setReceiver(const std::string receiver) { this->receiver = receiver; }
	std::string getReceiver() const { return receiver; }
	void setTitle(const std::string title) { this->title = title; }
	std::string getTitle() const { return title; }
	void setBody(const std::string body) { this->body = body; }
	std::string getBody() const { return body; }
	void setType(const std::string type) { this->type = type; }
	std::string getType() const { return type; }
	void setUrl(const std::string url) { this->url = url; }
	std::string getUrl() const { return url; }
	void setGenerateTime(const std::string generateTime) { this->generateTime = generateTime; }
	std::string getGenerateTime() const { return generateTime; }
	void setExpiration(const std::string expiration) { this->expiration = expiration; }
	std::string getExpiration() const { return expiration; }

private:
	std::string sender; // ��Ϣ�������˺�����(me�������Լ����͵���Ϣ)
	std::string receiver; // ��Ϣ������(�˺����ơ��绰�����Email��ַ�Կɣ������ڷ���)
	std::string title; // ��Ϣ����(��ѡ)
	std::string body; // ��Ϣ����
	std::string type; // ��Ϣ����(text/xml/html)
	std::string url; // ��Ϣ����URL(��ѡ)
	std::string generateTime; // ��Ϣ����ʱ��(yyyyMMddHHmmss)
	std::string expiration; // ��Ϣ����ʱ��(��ѡ����ʽͬ��)
	std::vector<Attachment> attachments; // ��Ϣ����
};
