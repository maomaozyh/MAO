-- 敏感词表
DROP TABLE IF EXISTS sys_sensitive_word;
CREATE TABLE sys_sensitive_word (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  word VARCHAR(255) NOT NULL COMMENT '敏感词',
  category VARCHAR(50) DEFAULT 'OTHER' COMMENT '分类：POLITICS-政治，PORN-色情，VIOLENCE-暴力，AD-广告，INSULT-辱骂，OTHER-其他',
  enabled TINYINT DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  isDelete TINYINT DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_word (word),
  KEY idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表';

-- 初始化一些常见敏感词
INSERT INTO sys_sensitive_word (word, category, remark) VALUES
('fuck', 'PORN', '英文脏话'),
('shit', 'PORN', '英文脏话'),
('傻逼', 'INSULT', '辱骂'),
('操你', 'INSULT', '辱骂'),
('草你', 'INSULT', '辱骂'),
('尼玛', 'INSULT', '辱骂'),
('你妈', 'INSULT', '辱骂'),
('垃圾', 'INSULT', '辱骂'),
('废物', 'INSULT', '辱骂'),
('去死', 'VIOLENCE', '暴力'),
('自杀', 'VIOLENCE', '暴力倾向'),
('加微信', 'AD', '广告'),
('加QQ', 'AD', '广告'),
('联系我', 'AD', '广告'),
('微信号', 'AD', '广告'),
('赚钱', 'AD', '广告'),
('兼职', 'AD', '广告');
