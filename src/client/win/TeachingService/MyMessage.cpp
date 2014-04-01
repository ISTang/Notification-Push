#include "StdAfx.h"
#include "MyMessage.h"
#include "json/json.h"

MyMessage::Attachment::Attachment(const std::string& title, const std::string& type, const std::string& filename, const std::string& url)
{
	this->title = title;
	this->type = type;
	this->filename = filename;
	this->url = url;
}

MyMessage::Attachment::Attachment()
{
}

MyMessage::MyMessage(const std::string& sender, const std::string& receiver, const std::string& title, const std::string& body,
		const std::string& type, const std::string& url, const std::string& generateTime, const std::string& expiration,
		const std::vector<Attachment>& attachments)
{
	this->sender = sender;
	this->receiver = receiver;
	this->title = title;
	this->body = body;
	this->type = type;
	this->url = url;
	this->generateTime = generateTime;
	this->expiration = expiration;
	this->attachments = attachments;
}

MyMessage::MyMessage(const std::string& title, const std::string& body, const std::string& type, const std::string& url,
		const std::string& generateTime, const std::string& expiration, const std::vector<Attachment>& attachments)
{
	MyMessage("", "", title, body, type, url, generateTime, expiration, attachments);
}

MyMessage::MyMessage(const std::string& body, const std::string& generateTime)
{
	this->body = body;
	this->generateTime = generateTime;
}

MyMessage MyMessage::parse(const std::string& jsonText)
{
	Json::Value root;
	Json::Reader reader;
	bool parsingSuccessful = reader.parse(jsonText, root);
	if (!parsingSuccessful)
	{
		throw "Invalid json text!";
	}

	MyMessage msg;
	//
	Json::Value sender = root.get("sender_name", "");
	if (!sender.isNull()) msg.sender = sender.asString();
	//
	Json::Value receiver = root.get("receiver", "");
	if (!receiver.isNull()) msg.receiver = receiver.asString();
	//
	Json::Value title = root.get("title", "");
	if (!title.isNull()) msg.title = title.asString();
	//
	Json::Value body = root.get("body", "");
	if (!body.isNull()) msg.body = body.asString();
	//
	Json::Value type = root.get("type", "");
	if (!type.isNull()) msg.type = type.asString();
	//
	Json::Value url = root.get("url", "");
	if (!url.isNull()) msg.url = url.asString();
	//
	Json::Value generateTime = root.get("generate_time", "");
	if (!generateTime.isNull()) msg.url = generateTime.asString();
	//
	Json::Value expiration = root.get("expiration", "");
	if (!expiration.isNull()) msg.expiration = expiration.asString();

	Json::Value attachments = root.get("attachments", "");
	if (attachments.isArray())
	{
		std::vector<Attachment> vecAttachments;
		for (auto i = 0; i < attachments.size(); i++)
		{
			Json::Value attachment = attachments.get(i, "");
			vecAttachments.push_back(Attachment(attachment.get("title", "").asString(),
				attachment.get("type", "text").asString(),
				attachment.get("filename", "").asString(),
				attachment.get("url", "").asString()));
		}
		msg.attachments = vecAttachments;
	}

	return msg;
}

MyMessage::MyMessage(void)
{
}


MyMessage::~MyMessage(void)
{
}

std::string MyMessage::toJson()
{
	Json::Value root;
	if (sender!="") root["sender_name"] = sender;
	if (receiver != "") root["receiver"] = receiver;
	if (title != "") root["title"] = title;
	if (body != "") root["body"] = body;
	if (type != "") root["type"] = type;
	if (url != "") root["url"] = url;
	if (generateTime != "") root["generate_time"] = generateTime;
	if (expiration != "") root["expiration"] = expiration;

	for (auto i = attachments.begin(); i != attachments.end(); ++i)
	{
		Json::Value value;
		if (i->getTitle()!="") value["title"] = i->getTitle();
		if (i->getType()!="") value["type"] = i->getType();
		if (i->getFilename()!="") value["filename"] = i->getFilename();
		if (i->getUrl()!="") value["url"] = i->getUrl();
		root["attachments"].append(value);
	}

	Json::StyledWriter writer;
	return writer.write(root);
}
