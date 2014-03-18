#include "StdAfx.h"
#include "MyMessage.h"

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

MyMessage::MyMessage(void)
{
}


MyMessage::~MyMessage(void)
{
}
