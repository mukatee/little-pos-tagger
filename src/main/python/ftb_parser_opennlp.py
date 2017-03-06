errors = []
printing_empty = False

unique_tags = []

#range(1, 10) provides 1,2,3,4,5,6,7,8,9
for hundredks in range(1, 10):
#for millions in [1,2,3,4]:
    training = True
    skipped = 0
    processed = 0
    sentences = 0
    train_count = 0
    test_count = 0
    counter = 0
    onlp_line = ""
    print("starting to process "+str(hundredks)+"x100k")
    with open("onlp_errors.txt", "w", encoding="utf8") as err_f:
        with open("onlp_transformed_train_"+str(hundredks)+"x100k.conllx", "w", encoding="utf8") as train_f:
            with open("onlp_transformed_test_"+str(hundredks)+"x100.conllx", "w", encoding="utf8") as test_f:
#    print("starting to process "+str(millions)+"M")
#    with open("onlp_errors.txt", "w", encoding="utf8") as err_f:
#        with open("onlp_transformed_train_"+str(millions)+"M.conllx", "w", encoding="utf8") as train_f:
#            with open("onlp_transformed_test_"+str(millions)+"M.conllx", "w", encoding="utf8") as test_f:
                with open("ftb3.1.conllx", encoding="utf8") as in_f:
                    for line in in_f:
                        if line.startswith("<"):
                            skipped += 1
                            if not printing_empty:
                                if training:
                                    train_f.write(onlp_line+"\n")
                                    train_count += 1
                                else:
                                    test_f.write(onlp_line+"\n")
                                    test_count += 1

                                printing_empty = True
                                sentences += 1
                            onlp_line = ""
                        else:
                            if not printing_empty:
                                #add whitespace but not before first word or after last (the if statement)
                                onlp_line += " "
                            items = line.split("\t")
                            tag = items[4]
                            onlp_line += items[1]+"_" + tag
                            if tag not in unique_tags:
                                unique_tags.append(tag)
                            printing_empty = False
                            processed += 1
                        counter += 1
                        if training and sentences > hundredks*100000:
#                        if training and sentences > millions*1000000:
                            training = False
    print(str(hundredks)+"x100k done.")
#    print(str(millions)+"M done.")
    print("words:"+str(processed))
    print("sentences:"+str(sentences))
    print("train set:"+str(train_count))
    print("test set:"+str(test_count))
    unique_tags.sort()
    print("unique tags:"+str(unique_tags))
#print(errors)

#total in ftb3.1.conllx is about 76369439 words/symbols and about 4366956 sentences
#with a limit of 2 million sentences, we get about 34213820 words and 2000001 sentences

#unique tags:['A', 'Abbr', 'Adp', 'Adv', 'AgPrc', 'CC', 'CS', 'Forgn', 'Interj', 'N', 'NegPrc', 'Num', 'PrfPrc', 'Pron', 'PrsPrc', 'Punct', 'TrunCo', 'Unkwn', 'V']